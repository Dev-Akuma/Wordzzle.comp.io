package io.comp.backend.shared.matchmaking.service;

import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.auth.repository.UserRepository;
import io.comp.backend.shared.websocket.service.WebSocketSessionManager;
import io.comp.backend.games.wordzzle.service.WordzzleService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchmakerService {
    private static final Logger logger = LoggerFactory.getLogger(MatchmakerService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private WordzzleService wordzzleService;

    private final Map<String, QueueEntry> queue = new ConcurrentHashMap<>();

    @Data
    public static class QueueEntry {
        private final String username;
        private final Integer elo;
        private final Instant joinedAt;
    }

    public synchronized void joinQueue(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        
        if (!queue.containsKey(username)) {
            queue.put(username, new QueueEntry(username, user.getEloRating(), Instant.now()));
            logger.info("Player {} joined the queue (Elo: {})", username, user.getEloRating());
        }
    }

    public synchronized void leaveQueue(String username) {
        if (queue.remove(username) != null) {
            logger.info("Player {} left the queue", username);
        }
    }

    public boolean isInQueue(String username) {
        return queue.containsKey(username);
    }

    @Scheduled(fixedDelay = 2000)
    public synchronized void matchPlayers() {
        if (queue.isEmpty()) {
            return;
        }

        // Check if any human player has waited for 10 seconds or longer
        for (QueueEntry entry : queue.values()) {
            long waitTime = Duration.between(entry.getJoinedAt(), Instant.now()).toSeconds();
            if (waitTime >= 10) {
                if (!sessionManager.isSessionActive(entry.getUsername())) {
                    queue.remove(entry.getUsername());
                    logger.info("Removing inactive player {} from queue", entry.getUsername());
                    continue;
                }

                // Match with a bot!
                queue.remove(entry.getUsername());
                matchWithBot(entry.getUsername(), entry.getElo());
                return;
            }
        }

        if (queue.size() < 2) {
            return;
        }

        logger.debug("Running matchmaker... Current queue size: {}", queue.size());

        List<QueueEntry> entries = new ArrayList<>(queue.values());
        // Sort by Elo rating
        entries.sort(Comparator.comparingInt(QueueEntry::getElo));

        Set<String> matchedUsernames = new HashSet<>();

        for (int i = 0; i < entries.size() - 1; i++) {
            QueueEntry p1 = entries.get(i);
            if (matchedUsernames.contains(p1.getUsername())) continue;

            for (int j = i + 1; j < entries.size(); j++) {
                QueueEntry p2 = entries.get(j);
                if (matchedUsernames.contains(p2.getUsername())) continue;

                // Calculate wait time
                long waitTime1 = Duration.between(p1.getJoinedAt(), Instant.now()).toSeconds();
                long waitTime2 = Duration.between(p2.getJoinedAt(), Instant.now()).toSeconds();
                long maxWaitTime = Math.max(waitTime1, waitTime2);

                // Allow Elo difference to expand over wait time
                int eloDiff = Math.abs(p1.getElo() - p2.getElo());
                int maxEloDiff = (int) (100 + maxWaitTime * 10);

                if (eloDiff <= maxEloDiff) {
                    if (!sessionManager.isSessionActive(p1.getUsername())) {
                        queue.remove(p1.getUsername());
                        logger.info("Removing inactive player {} from queue", p1.getUsername());
                        break;
                    }
                    if (!sessionManager.isSessionActive(p2.getUsername())) {
                        queue.remove(p2.getUsername());
                        logger.info("Removing inactive player {} from queue", p2.getUsername());
                        continue;
                    }

                    // Match found!
                    matchedUsernames.add(p1.getUsername());
                    matchedUsernames.add(p2.getUsername());

                    queue.remove(p1.getUsername());
                    queue.remove(p2.getUsername());

                    logger.info("Match found between {} ({}) and {} ({})", 
                            p1.getUsername(), p1.getElo(), p2.getUsername(), p2.getElo());

                    createAndStartMatch(p1.getUsername(), p2.getUsername());
                    break;
                }
            }
        }
    }

    private void matchWithBot(String humanUsername, int humanElo) {
        try {
            List<User> bots = userRepository.findByIsBotTrue();
            if (bots.isEmpty()) {
                logger.error("No bot accounts found in database!");
                // Re-queue human if no bots found
                joinQueue(humanUsername);
                return;
            }

            // Find closest bot
            User bestBot = bots.stream()
                    .min(Comparator.comparingInt(b -> Math.abs(b.getEloRating() - humanElo)))
                    .orElse(bots.get(0));

            logger.info("Matching human player {} ({}) with AI bot {} ({}) due to timeout", 
                    humanUsername, humanElo, bestBot.getUsername(), bestBot.getEloRating());

            wordzzleService.initializeMatch(humanUsername, bestBot.getUsername());
        } catch (Exception e) {
            logger.error("Failed to start bot match for {}", humanUsername, e);
            joinQueue(humanUsername);
        }
    }

    private void createAndStartMatch(String username1, String username2) {
        try {
            wordzzleService.initializeMatch(username1, username2);
        } catch (Exception e) {
            logger.error("Failed to start match between {} and {}", username1, username2, e);
            joinQueue(username1);
            joinQueue(username2);
        }
    }
}
