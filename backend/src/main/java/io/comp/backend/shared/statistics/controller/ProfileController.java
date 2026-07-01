package io.comp.backend.shared.statistics.controller;

import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.auth.repository.UserRepository;
import io.comp.backend.shared.statistics.dto.*;
import io.comp.backend.shared.statistics.model.Match;
import io.comp.backend.shared.statistics.model.MatchPlayerState;
import io.comp.backend.shared.statistics.model.Statistics;
import io.comp.backend.shared.statistics.repository.MatchPlayerStateRepository;
import io.comp.backend.shared.statistics.repository.MatchRepository;
import io.comp.backend.shared.statistics.repository.StatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfileController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchPlayerStateRepository matchPlayerStateRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found in database"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getUserProfile() {
        User user = getCurrentUser();
        Statistics stats = statisticsRepository.findByUser(user)
                .orElseGet(() -> statisticsRepository.save(Statistics.builder().user(user).build()));

        var statsDto = ProfileResponse.StatsDto.builder()
                .gamesPlayed(stats.getGamesPlayed())
                .wins(stats.getWins())
                .losses(stats.getLosses())
                .winStreak(stats.getWinStreak())
                .avgGuesses(stats.getAvgGuesses())
                .avgSolveTimeSeconds(stats.getAvgSolveTimeSeconds())
                .build();

        return ResponseEntity.ok(ProfileResponse.builder()
                .username(user.getUsername())
                .eloRating(user.getEloRating())
                .stats(statsDto)
                .build());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        List<User> topUsers = userRepository.findTop20ByOrderByEloRatingDesc();
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        
        int rank = 1;
        for (User u : topUsers) {
            leaderboard.add(new LeaderboardEntry(rank++, u.getUsername(), u.getEloRating()));
        }
        
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/matches/history")
    public ResponseEntity<List<MatchHistoryEntry>> getMatchHistory() {
        User user = getCurrentUser();
        List<Match> matches = matchRepository.findUserMatchHistory(user);
        List<MatchHistoryEntry> history = new ArrayList<>();

        for (Match m : matches) {
            User opponent = null;
            if (m.getPlayer1() != null && m.getPlayer1().getId().equals(user.getId())) {
                opponent = m.getPlayer2();
            } else {
                opponent = m.getPlayer1();
            }
            MatchPlayerState userState = matchPlayerStateRepository.findByMatchAndUser(m, user).orElse(null);
            
            String result = "UNFINISHED";
            if ("COMPLETED".equals(m.getStatus())) {
                if (m.getWinner() == null) {
                    result = "DRAW";
                } else if (m.getWinner().getId().equals(user.getId())) {
                    result = "WIN";
                } else {
                    result = "LOSS";
                }
            } else if ("DRAW".equals(m.getStatus())) {
                result = "DRAW";
            } else if ("FORFEITED".equals(m.getStatus())) {
                if (m.getWinner() != null && m.getWinner().getId().equals(user.getId())) {
                    result = "WIN";
                } else {
                    result = "LOSS";
                }
            }

            Integer solveTime = (userState != null) ? userState.getSolveTimeSeconds() : null;
            Integer attempts = (userState != null) ? userState.getAttemptsCount() : 0;
            Integer ratingChange = (userState != null) ? userState.getRatingChange() : 0;

            history.add(MatchHistoryEntry.builder()
                    .matchId(m.getId().toString())
                    .opponentName(opponent != null ? opponent.getUsername() : "Unknown")
                    .result(result)
                    .solveTimeSeconds(solveTime)
                    .attempts(attempts)
                    .ratingChange(ratingChange)
                    .playedAt(m.getCreatedAt())
                    .build());
        }

        return ResponseEntity.ok(history);
    }
}
