package io.comp.backend.games.wordzzle.service;

import io.comp.backend.games.wordzzle.model.FeedbackType;
import io.comp.backend.games.wordzzle.model.WordzzleGameSession;
import io.comp.backend.games.wordzzle.repository.WordRepository;
import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.auth.repository.UserRepository;
import io.comp.backend.shared.rating.service.EloRatingService;
import io.comp.backend.shared.statistics.model.Match;
import io.comp.backend.shared.statistics.model.MatchPlayerState;
import io.comp.backend.shared.statistics.model.RatingHistory;
import io.comp.backend.shared.statistics.model.Statistics;
import io.comp.backend.shared.statistics.repository.MatchPlayerStateRepository;
import io.comp.backend.shared.statistics.repository.MatchRepository;
import io.comp.backend.shared.statistics.repository.RatingHistoryRepository;
import io.comp.backend.shared.statistics.repository.StatisticsRepository;
import io.comp.backend.shared.websocket.service.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class WordzzleService {
    private static final Logger logger = LoggerFactory.getLogger(WordzzleService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchPlayerStateRepository matchPlayerStateRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private RatingHistoryRepository ratingHistoryRepository;

    @Autowired
    private EloRatingService eloRatingService;

    @Autowired
    private WebSocketSessionManager sessionManager;

    private final Map<UUID, WordzzleGameSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static final List<String> FALLBACK_WORDS = Arrays.asList(
            "REACT", "APPLE", "CLOUD", "LIGHT", "CRANE", "TRAIN", "HOUSE", "WORLD", "MATCH", "BOARD"
    );

    private String getRandomWord() {
        return wordRepository.findRandomWord()
                .map(w -> w.getWord().toUpperCase())
                .orElseGet(() -> {
                    Random rand = new Random();
                    return FALLBACK_WORDS.get(rand.nextInt(FALLBACK_WORDS.size())).toUpperCase();
                });
    }

    @Transactional
    public void initializeMatch(String username1, String username2) {
        User p1 = userRepository.findByUsername(username1).orElseThrow();
        User p2 = userRepository.findByUsername(username2).orElseThrow();

        String hiddenWord = getRandomWord();

        // 1. Create Match database record
        Match match = Match.builder()
                .gameType("WORDZZLE")
                .status("PENDING")
                .hiddenWord(hiddenWord)
                .player1(p1)
                .player2(p2)
                .build();
        match = matchRepository.save(match);

        // 2. Create MatchPlayerStates
        MatchPlayerState mps1 = MatchPlayerState.builder().match(match).user(p1).build();
        MatchPlayerState mps2 = MatchPlayerState.builder().match(match).user(p2).build();
        matchPlayerStateRepository.save(mps1);
        matchPlayerStateRepository.save(mps2);

        // 3. Create active game session
        WordzzleGameSession session = new WordzzleGameSession(match.getId(), hiddenWord, username1, username2);
        activeSessions.put(match.getId(), session);

        // 4. Send MATCH_FOUND WebSocket notifications
        UUID matchId = match.getId();
        sessionManager.sendMessage(username1, "MATCH_FOUND", Map.of(
                "matchId", matchId,
                "opponentName", username2,
                "opponentElo", p2.getEloRating(),
                "countdownSeconds", 3
        ));
        sessionManager.sendMessage(username2, "MATCH_FOUND", Map.of(
                "matchId", matchId,
                "opponentName", username1,
                "opponentElo", p1.getEloRating(),
                "countdownSeconds", 3
        ));

        // 5. Schedule GAME_START countdown
        scheduler.schedule(() -> startGame(matchId), 3, TimeUnit.SECONDS);
    }

    @Transactional
    public void startGame(UUID matchId) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session == null) return;

        session.setStartTime(Instant.now());
        
        // Update database Match state
        Match match = matchRepository.findById(matchId).orElseThrow();
        match.setStatus("IN_PROGRESS");
        match.setStartedAt(LocalDateTime.now());
        matchRepository.save(match);

        // Notify both players
        sessionManager.sendMessage(session.getPlayer1Username(), "GAME_START", Map.of(
                "matchId", matchId,
                "roundTimeLimitSeconds", 300
        ));
        sessionManager.sendMessage(session.getPlayer2Username(), "GAME_START", Map.of(
                "matchId", matchId,
                "roundTimeLimitSeconds", 300
        ));

        // Schedule timeout at 5 minutes
        scheduler.schedule(() -> handleTimeout(matchId), 300, TimeUnit.SECONDS);

        // Trigger bot moves if applicable
        triggerBotMoveIfNeeded(matchId);
    }

    public void submitGuess(UUID matchId, String username, String guess) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session == null || session.isEnded()) {
            throw new IllegalArgumentException("Session not found or already finished.");
        }

        boolean isPlayer1 = session.getPlayer1Username().equals(username);
        boolean isPlayer2 = session.getPlayer2Username().equals(username);
        if (!isPlayer1 && !isPlayer2) {
            throw new IllegalArgumentException("Unauthorized player for this match.");
        }

        guess = guess.trim().toUpperCase();
        if (guess.length() != 5) {
            throw new IllegalArgumentException("Guess must be exactly 5 letters long.");
        }

        // Validate dictionary if populated
        if (wordRepository.count() > 0 && !wordRepository.existsByWord(guess)) {
            sessionManager.sendMessage(username, "GUESS_ERROR", Map.of("message", "Not in word list"));
            return;
        }

        List<FeedbackType> feedback = checkWordleGuess(guess, session.getHiddenWord());
        boolean solved = guess.equals(session.getHiddenWord());
        int attemptsCount;

        if (isPlayer1) {
            session.getPlayer1Guesses().add(guess);
            session.getPlayer1Feedback().add(feedback);
            session.setPlayer1Attempts(session.getPlayer1Attempts() + 1);
            attemptsCount = session.getPlayer1Attempts();
            if (solved) {
                session.setPlayer1Solved(true);
                session.setPlayer1SolveTimeSeconds((int) Duration.between(session.getStartTime(), Instant.now()).toSeconds());
                session.setPlayer1Finished(true);
            } else if (attemptsCount >= 6) {
                session.setPlayer1Finished(true);
            }
        } else {
            session.getPlayer2Guesses().add(guess);
            session.getPlayer2Feedback().add(feedback);
            session.setPlayer2Attempts(session.getPlayer2Attempts() + 1);
            attemptsCount = session.getPlayer2Attempts();
            if (solved) {
                session.setPlayer2Solved(true);
                session.setPlayer2SolveTimeSeconds((int) Duration.between(session.getStartTime(), Instant.now()).toSeconds());
                session.setPlayer2Finished(true);
            } else if (attemptsCount >= 6) {
                session.setPlayer2Finished(true);
            }
        }

        // Persist progress to Database
        updatePlayerStateInDb(matchId, username, attemptsCount, solved, guess, session);

        // Send feedback to guesser
        sessionManager.sendMessage(username, "SERVER_GUESS_RESULT", Map.of(
                "guess", guess,
                "feedback", feedback,
                "attemptsCount", attemptsCount,
                "solved", solved,
                "remainingGuesses", 6 - attemptsCount
        ));

        // Send progress to opponent (Grid progress, not the word)
        String opponent = isPlayer1 ? session.getPlayer2Username() : session.getPlayer1Username();
        sessionManager.sendMessage(opponent, "SERVER_OPPONENT_PROGRESS", Map.of(
                "attemptsCount", attemptsCount,
                "gridRow", feedback,
                "solved", solved
        ));

        // If both finished, end match
        if (session.isPlayer1Finished() && session.isPlayer2Finished()) {
            endMatch(session, false, null);
        }
    }

    @Transactional
    protected void updatePlayerStateInDb(UUID matchId, String username, int attempts, boolean solved, String guess, WordzzleGameSession session) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Match match = matchRepository.findById(matchId).orElseThrow();
        MatchPlayerState mps = matchPlayerStateRepository.findByMatchAndUser(match, user).orElseThrow();

        mps.setAttemptsCount(attempts);
        mps.setCompleted(solved || attempts >= 6);
        if (solved) {
            mps.setSolveTimeSeconds((int) Duration.between(session.getStartTime(), Instant.now()).toSeconds());
        }
        
        // Save guess history
        String currentGuesses = mps.getGuesses();
        if (currentGuesses == null || currentGuesses.isEmpty()) {
            mps.setGuesses(guess);
        } else {
            mps.setGuesses(currentGuesses + "," + guess);
        }
        
        matchPlayerStateRepository.save(mps);
    }

    private void handleTimeout(UUID matchId) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session != null && !session.isEnded()) {
            logger.info("Timer expired for match {}. Forcing completion.", matchId);
            endMatch(session, true, null);
        }
    }

    @Transactional
    public void surrender(UUID matchId, String forfeitUsername) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session != null && !session.isEnded()) {
            logger.info("Player {} forfeited match {}", forfeitUsername, matchId);
            endMatch(session, false, forfeitUsername);
        }
    }

    @Transactional
    protected synchronized void endMatch(WordzzleGameSession session, boolean timeoutExpired, String forfeiter) {
        // Double check ended flag
        if (session.isEnded()) return;
        session.setEnded(true);
        activeSessions.remove(session.getMatchId());

        Match match = matchRepository.findById(session.getMatchId()).orElseThrow();
        User p1 = userRepository.findByUsername(session.getPlayer1Username()).orElseThrow();
        User p2 = userRepository.findByUsername(session.getPlayer2Username()).orElseThrow();

        MatchPlayerState mps1 = matchPlayerStateRepository.findByMatchAndUser(match, p1).orElseThrow();
        MatchPlayerState mps2 = matchPlayerStateRepository.findByMatchAndUser(match, p2).orElseThrow();

        User winner = null;
        String endReason = "SOLVED";

        if (forfeiter != null) {
            endReason = "FORFEITED";
            winner = forfeiter.equals(session.getPlayer1Username()) ? p2 : p1;
        } else if (timeoutExpired) {
            endReason = "TIMEOUT";
            winner = determineWinner(session);
        } else {
            winner = determineWinner(session);
        }

        // Update match status
        match.setStatus(winner == null ? "DRAW" : "COMPLETED");
        match.setWinner(winner);
        match.setEndedAt(LocalDateTime.now());
        matchRepository.save(match);

        // Update player completed states
        mps1.setCompleted(true);
        mps2.setCompleted(true);

        // Calculate Elo ratings change
        double scoreA = 0.5; // default draw
        if (winner != null) {
            scoreA = winner.getId().equals(p1.getId()) ? 1.0 : 0.0;
        }
        
        int[] eloChanges = eloRatingService.calculateElo(p1.getEloRating(), p2.getEloRating(), scoreA);
        int change1 = eloChanges[0];
        int change2 = eloChanges[1];

        // Apply Elo modifications
        p1.setEloRating(Math.max(100, p1.getEloRating() + change1));
        p2.setEloRating(Math.max(100, p2.getEloRating() + change2));
        userRepository.save(p1);
        userRepository.save(p2);

        // Record player rating changes
        mps1.setRatingChange(change1);
        mps2.setRatingChange(change2);
        matchPlayerStateRepository.save(mps1);
        matchPlayerStateRepository.save(mps2);

        // Save RatingHistory records
        ratingHistoryRepository.save(RatingHistory.builder().user(p1).match(match).ratingAfter(p1.getEloRating()).changeAmount(change1).build());
        ratingHistoryRepository.save(RatingHistory.builder().user(p2).match(match).ratingAfter(p2.getEloRating()).changeAmount(change2).build());

        // Update Stats records
        updateUserStatistics(p1, winner == null ? 0 : (winner.getId().equals(p1.getId()) ? 1 : -1), session.getPlayer1Attempts(), session.getPlayer1SolveTimeSeconds(), session.isPlayer1Solved());
        updateUserStatistics(p2, winner == null ? 0 : (winner.getId().equals(p2.getId()) ? 1 : -1), session.getPlayer2Attempts(), session.getPlayer2SolveTimeSeconds(), session.isPlayer2Solved());

        // Notify clients of match ending
        String winnerName = winner != null ? winner.getUsername() : null;
        sessionManager.sendMessage(session.getPlayer1Username(), "MSG_GAME_END", Map.of(
                "winnerUsername", winnerName == null ? "DRAW" : winnerName,
                "endReason", endReason,
                "targetWord", session.getHiddenWord(),
                "newElo", p1.getEloRating(),
                "eloChange", change1,
                "playerScore", Map.of("attempts", session.getPlayer1Attempts(), "timeSeconds", session.getPlayer1SolveTimeSeconds() == null ? 300 : session.getPlayer1SolveTimeSeconds(), "solved", session.isPlayer1Solved()),
                "opponentScore", Map.of("attempts", session.getPlayer2Attempts(), "timeSeconds", session.getPlayer2SolveTimeSeconds() == null ? 300 : session.getPlayer2SolveTimeSeconds(), "solved", session.isPlayer2Solved())
        ));
        sessionManager.sendMessage(session.getPlayer2Username(), "MSG_GAME_END", Map.of(
                "winnerUsername", winnerName == null ? "DRAW" : winnerName,
                "endReason", endReason,
                "targetWord", session.getHiddenWord(),
                "newElo", p2.getEloRating(),
                "eloChange", change2,
                "playerScore", Map.of("attempts", session.getPlayer2Attempts(), "timeSeconds", session.getPlayer2SolveTimeSeconds() == null ? 300 : session.getPlayer2SolveTimeSeconds(), "solved", session.isPlayer2Solved()),
                "opponentScore", Map.of("attempts", session.getPlayer1Attempts(), "timeSeconds", session.getPlayer1SolveTimeSeconds() == null ? 300 : session.getPlayer1SolveTimeSeconds(), "solved", session.isPlayer1Solved())
        ));
    }

    private User determineWinner(WordzzleGameSession session) {
        User p1 = userRepository.findByUsername(session.getPlayer1Username()).orElseThrow();
        User p2 = userRepository.findByUsername(session.getPlayer2Username()).orElseThrow();

        if (session.isPlayer1Solved() && session.isPlayer2Solved()) {
            // Both solved
            if (session.getPlayer1Attempts() < session.getPlayer2Attempts()) return p1;
            if (session.getPlayer2Attempts() < session.getPlayer1Attempts()) return p2;
            
            // Guesses are equal, compare times
            if (session.getPlayer1SolveTimeSeconds() < session.getPlayer2SolveTimeSeconds()) return p1;
            if (session.getPlayer2SolveTimeSeconds() < session.getPlayer1SolveTimeSeconds()) return p2;
            
            return null; // Draw
        } else if (session.isPlayer1Solved()) {
            return p1;
        } else if (session.isPlayer2Solved()) {
            return p2;
        } else {
            // Neither solved - compare best attempt progress
            int progress1 = calculateMaxCorrectLetters(session.getPlayer1Feedback());
            int progress2 = calculateMaxCorrectLetters(session.getPlayer2Feedback());
            if (progress1 > progress2) return p1;
            if (progress2 > progress1) return p2;

            // If correct green counts are equal, check yellow present counts
            int yellow1 = calculateMaxPresentLetters(session.getPlayer1Feedback());
            int yellow2 = calculateMaxPresentLetters(session.getPlayer2Feedback());
            if (yellow1 > yellow2) return p1;
            if (yellow2 > yellow1) return p2;

            return null; // Draw
        }
    }

    private int calculateMaxCorrectLetters(List<List<FeedbackType>> feedbackList) {
        int max = 0;
        for (List<FeedbackType> row : feedbackList) {
            int correct = 0;
            for (FeedbackType f : row) {
                if (f == FeedbackType.CORRECT) correct++;
            }
            max = Math.max(max, correct);
        }
        return max;
    }

    private int calculateMaxPresentLetters(List<List<FeedbackType>> feedbackList) {
        int max = 0;
        for (List<FeedbackType> row : feedbackList) {
            int present = 0;
            for (FeedbackType f : row) {
                if (f == FeedbackType.PRESENT) present++;
            }
            max = Math.max(max, present);
        }
        return max;
    }

    private void updateUserStatistics(User user, int outcome, int attempts, Integer solveTime, boolean solved) {
        Statistics stats = statisticsRepository.findByUser(user)
                .orElseGet(() -> statisticsRepository.save(Statistics.builder().user(user).build()));

        stats.setGamesPlayed(stats.getGamesPlayed() + 1);
        if (outcome > 0) {
            stats.setWins(stats.getWins() + 1);
            stats.setWinStreak(stats.getWinStreak() + 1);
        } else if (outcome < 0) {
            stats.setLosses(stats.getLosses() + 1);
            stats.setWinStreak(0);
        } else {
            stats.setWinStreak(0); // break streak on draw
        }

        // Update avg guesses (only include solved games, or count failed games as 6 attempts)
        double currentGuessesSum = stats.getAvgGuesses() * (stats.getGamesPlayed() - 1);
        int addedGuesses = solved ? attempts : 6;
        stats.setAvgGuesses((currentGuessesSum + addedGuesses) / stats.getGamesPlayed());

        // Update avg solve time (only update if solved)
        if (solved && solveTime != null) {
            double currentSolveTimeSum = stats.getAvgSolveTimeSeconds() * (stats.getWins() - 1); // average of won games, or played games. Let's average over won games for simplicity.
            int winsCount = stats.getWins();
            if (winsCount > 0) {
                stats.setAvgSolveTimeSeconds((currentSolveTimeSum + solveTime) / winsCount);
            }
        }

        statisticsRepository.save(stats);
    }

    public List<FeedbackType> checkWordleGuess(String guess, String target) {
        FeedbackType[] results = new FeedbackType[5];
        Arrays.fill(results, FeedbackType.ABSENT);

        Map<Character, Integer> targetLetterCounts = new HashMap<>();
        for (char c : target.toCharArray()) {
            targetLetterCounts.put(c, targetLetterCounts.getOrDefault(c, 0) + 1);
        }

        // Green pass
        for (int i = 0; i < 5; i++) {
            char gChar = guess.charAt(i);
            char tChar = target.charAt(i);
            if (gChar == tChar) {
                results[i] = FeedbackType.CORRECT;
                targetLetterCounts.put(gChar, targetLetterCounts.get(gChar) - 1);
            }
        }

        // Yellow pass
        for (int i = 0; i < 5; i++) {
            if (results[i] == FeedbackType.CORRECT) continue;
            char gChar = guess.charAt(i);
            if (targetLetterCounts.getOrDefault(gChar, 0) > 0) {
                results[i] = FeedbackType.PRESENT;
                targetLetterCounts.put(gChar, targetLetterCounts.get(gChar) - 1);
            }
        }

        return Arrays.asList(results);
    }

    public WordzzleGameSession getActiveSessionByPlayer(String username) {
        for (WordzzleGameSession session : activeSessions.values()) {
            if (session.getPlayer1Username().equals(username) || session.getPlayer2Username().equals(username)) {
                return session;
            }
        }
        return null;
    }

    private void triggerBotMoveIfNeeded(UUID matchId) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session == null || session.isEnded()) return;

        User p1 = userRepository.findByUsername(session.getPlayer1Username()).orElse(null);
        User p2 = userRepository.findByUsername(session.getPlayer2Username()).orElse(null);

        if (p1 != null && p1.isBotAccount()) {
            scheduleBotGuess(matchId, p1.getUsername());
        }
        if (p2 != null && p2.isBotAccount()) {
            scheduleBotGuess(matchId, p2.getUsername());
        }
    }

    private void scheduleBotGuess(UUID matchId, String botUsername) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session == null || session.isEnded()) return;

        Random rand = new Random();
        int delay = 10 + rand.nextInt(11); // 10 to 20 seconds delay

        scheduler.schedule(() -> executeBotGuess(matchId, botUsername), delay, TimeUnit.SECONDS);
    }

    private void executeBotGuess(UUID matchId, String botUsername) {
        WordzzleGameSession session = activeSessions.get(matchId);
        if (session == null || session.isEnded()) return;

        boolean isPlayer1 = session.getPlayer1Username().equals(botUsername);
        boolean isFinished = isPlayer1 ? session.isPlayer1Finished() : session.isPlayer2Finished();
        if (isFinished) return;

        String guessWord = generateBotGuess(session, botUsername);

        try {
            logger.info("Bot {} is submitting guess: {} for match {}", botUsername, guessWord, matchId);
            submitGuess(matchId, botUsername, guessWord);
        } catch (Exception e) {
            logger.error("Bot {} failed to submit guess", botUsername, e);
        }

        WordzzleGameSession updatedSession = activeSessions.get(matchId);
        if (updatedSession != null && !updatedSession.isEnded()) {
            boolean updatedIsFinished = isPlayer1 ? updatedSession.isPlayer1Finished() : updatedSession.isPlayer2Finished();
            if (!updatedIsFinished) {
                scheduleBotGuess(matchId, botUsername);
            }
        }
    }

    private String generateBotGuess(WordzzleGameSession session, String botUsername) {
        boolean isPlayer1 = session.getPlayer1Username().equals(botUsername);
        int attemptsCount = isPlayer1 ? session.getPlayer1Attempts() : session.getPlayer2Attempts();
        int attempt = attemptsCount + 1;

        Random rand = new Random();
        double solveProbability = 0.0;

        switch (attempt) {
            case 1 -> solveProbability = 0.0;
            case 2 -> solveProbability = 0.15;
            case 3 -> solveProbability = 0.30;
            case 4 -> solveProbability = 0.50;
            case 5 -> solveProbability = 0.75;
            default -> solveProbability = 0.90;
        }

        if (rand.nextDouble() < solveProbability) {
            return session.getHiddenWord();
        }

        return getRandomWord();
    }
}
