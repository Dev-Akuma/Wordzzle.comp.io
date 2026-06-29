package io.comp.backend.shared.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Autowired
    private ObjectMapper objectMapper;

    public void registerSession(String username, WebSocketSession session) {
        WebSocketSession oldSession = sessions.put(username, session);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
                logger.info("Closed previous session for user {}", username);
            } catch (IOException e) {
                logger.error("Failed to close old session for user {}", username, e);
            }
        }
        logger.info("Registered active WebSocket session for user {}", username);
    }

    public void removeSession(String username) {
        sessions.remove(username);
        logger.info("Removed WebSocket session for user {}", username);
    }

    public boolean isSessionActive(String username) {
        WebSocketSession session = sessions.get(username);
        return session != null && session.isOpen();
    }

    public void sendMessage(String username, String type, Object payload) {
        WebSocketSession session = sessions.get(username);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", type,
                        "payload", payload
                );
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                logger.debug("Sent message to {}: type={}, payload={}", username, type, payload);
            } catch (IOException e) {
                logger.error("Failed to send message to user {}", username, e);
            }
        } else {
            logger.warn("Could not send message to {} - session inactive", username);
        }
    }
}
