package com.assignment.market_data_service.websocket;

import com.assignment.market_data_service.service.OrderBookServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ClientWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientWebSocketHandler.class);
    private final OrderBookServiceImpl orderBookService;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public ClientWebSocketHandler(OrderBookServiceImpl orderBookService, SessionManager sessionManager, ObjectMapper objectMapper) {
        this.orderBookService = orderBookService;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Browser WebSocket connection established: {}", session.getId());
        String username = getUsername(session);
        if (username != null) {
            sessionManager.registerSession(username, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message from client: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            String action = root.path("action").asText();
            String symbol = root.path("symbol").asText();

            if ("SUBSCRIBE".equalsIgnoreCase(action)) {
                orderBookService.handleClientSubscribe(session, symbol);
            } else if ("UNSUBSCRIBE".equalsIgnoreCase(action)) {
                orderBookService.handleClientUnsubscribe(session, symbol);
            }
        } catch (Exception e) {
            log.error("Failed to parse client message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Browser WebSocket connection closed: {}", session.getId());
        String username = getUsername(session);
        if (username != null) {
            sessionManager.removeSession(username, session);
        }
        orderBookService.handleSessionDisconnect(session);
    }

    private String getUsername(WebSocketSession session) {
        java.security.Principal principal = session.getPrincipal();
        return principal != null ? principal.getName() : null;
    }
}
