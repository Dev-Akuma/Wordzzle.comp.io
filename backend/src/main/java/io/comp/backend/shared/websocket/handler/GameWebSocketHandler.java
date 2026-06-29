package io.comp.backend.shared.websocket.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.comp.backend.games.wordzzle.service.WordzzleService;
import io.comp.backend.games.wordzzle.model.WordzzleGameSession;
import io.comp.backend.shared.auth.jwt.JwtUtils;
import io.comp.backend.shared.matchmaking.service.MatchmakerService;
import io.comp.backend.shared.websocket.service.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private MatchmakerService matchmakerService;

    @Autowired
    private WordzzleService wordzzleService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, ScheduledFuture<?>> disconnectTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            token = query.split("token=")[1].split("&")[0];
        }

        if (token != null && jwtUtils.validateJwtToken(token)) {
            String username = jwtUtils.getUserNameFromJwtToken(token);
            session.getAttributes().put("username", username);
            
            ScheduledFuture<?> task = disconnectTasks.remove(username);
            if (task != null) {
                task.cancel(false);
                logger.info("Player {} reconnected. Cancelled forfeit timer.", username);
            }
            
            sessionManager.registerSession(username, session);
            
            // Check if player has an active game to reconnect to
            WordzzleGameSession gameSession = wordzzleService.getActiveSessionByPlayer(username);
            if (gameSession != null && !gameSession.isEnded()) {
                boolean isP1 = gameSession.getPlayer1Username().equals(username);
                int attempts = isP1 ? gameSession.getPlayer1Attempts() : gameSession.getPlayer2Attempts();
                sessionManager.sendMessage(username, "MSG_RECONNECT", Map.of(
                        "matchId", gameSession.getMatchId(),
                        "opponentName", isP1 ? gameSession.getPlayer2Username() : gameSession.getPlayer1Username(),
                        "guesses", isP1 ? gameSession.getPlayer1Guesses() : gameSession.getPlayer2Guesses(),
                        "feedback", isP1 ? gameSession.getPlayer1Feedback() : gameSession.getPlayer2Feedback(),
                        "attemptsCount", attempts,
                        "solved", isP1 ? gameSession.isPlayer1Solved() : gameSession.isPlayer2Solved()
                ));
            }
        } else {
            logger.warn("WebSocket connection rejected: invalid JWT token");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        try {
            String payloadStr = message.getPayload();
            Map<String, Object> msgMap = objectMapper.readValue(payloadStr, new TypeReference<Map<String, Object>>() {});
            String type = (String) msgMap.get("type");
            
            if ("HEARTBEAT".equalsIgnoreCase(type)) {
                sessionManager.sendMessage(username, "HEARTBEAT", Map.of("timestamp", System.currentTimeMillis()));
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) msgMap.get("payload");
            if (payload == null) return;

            UUID matchId = UUID.fromString((String) payload.get("matchId"));

            if ("CLIENT_SUBMIT_GUESS".equalsIgnoreCase(type)) {
                String guess = (String) payload.get("guess");
                wordzzleService.submitGuess(matchId, username, guess);
            } else if ("CLIENT_SURRENDER".equalsIgnoreCase(type)) {
                wordzzleService.surrender(matchId, username);
            }
        } catch (Exception e) {
            logger.error("Error processing websocket message from user {}", username, e);
            sessionManager.sendMessage(username, "GUESS_ERROR", Map.of("message", "Invalid input or process error"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            logger.warn("WebSocket connection closed for player {}", username);
            sessionManager.removeSession(username);
            matchmakerService.leaveQueue(username);

            WordzzleGameSession gameSession = wordzzleService.getActiveSessionByPlayer(username);
            if (gameSession != null && !gameSession.isEnded()) {
                ScheduledFuture<?> task = scheduler.schedule(() -> {
                    logger.warn("Forfeit grace period expired for disconnected player {}", username);
                    wordzzleService.surrender(gameSession.getMatchId(), username);
                    disconnectTasks.remove(username);
                }, 20, TimeUnit.SECONDS);
                
                disconnectTasks.put(username, task);
                logger.info("Scheduled 20s forfeit grace period for disconnected player {}", username);
            }
        }
    }
}
