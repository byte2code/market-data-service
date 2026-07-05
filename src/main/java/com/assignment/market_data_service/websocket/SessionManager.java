package com.assignment.market_data_service.websocket;

import com.assignment.market_data_service.service.OrderBookServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final OrderBookServiceImpl orderBookService;

    public SessionManager(@Lazy OrderBookServiceImpl orderBookService) {
        this.orderBookService = orderBookService;
    }

    public void registerSession(String username, WebSocketSession session) {
        if (username == null || username.isEmpty()) {
            return;
        }

        WebSocketSession existingSession = activeSessions.put(username, session);
        if (existingSession != null && existingSession.isOpen() && !existingSession.getId().equals(session.getId())) {
            log.warn("User {} connected again. Terminating previous session: {}", username, existingSession.getId());
            orderBookService.handleSessionDisconnect(existingSession);
            try {
                existingSession.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.error("Error closing duplicated session for user " + username, e);
            }
        }
    }

    public void removeSession(String username, WebSocketSession session) {
        if (username == null || username.isEmpty() || session == null) {
            return;
        }
        boolean removed = activeSessions.remove(username, session);
        if (removed) {
            log.info("Successfully unregistered session {} for user {}", session.getId(), username);
        }
    }
}
