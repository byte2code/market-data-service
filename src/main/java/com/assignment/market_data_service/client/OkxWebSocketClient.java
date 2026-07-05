package com.assignment.market_data_service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Component
public class OkxWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(OkxWebSocketClient.class);
    private final String websocketUrl;
    private final ObjectMapper objectMapper;
    private final WebSocketClient client;
    private WebSocketSession okxSession;
    private Consumer<String> messageHandler;

    public OkxWebSocketClient(@Value("${okx.websocket-url}") String websocketUrl, ObjectMapper objectMapper) {
        this.websocketUrl = websocketUrl;
        this.objectMapper = objectMapper;
        this.client = new StandardWebSocketClient();
    }

    public synchronized void connect(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
        if (okxSession != null && okxSession.isOpen()) {
            return;
        }
        log.info("Connecting to OKX WebSocket API: {}", websocketUrl);
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    log.info("OKX WebSocket connection established");
                    okxSession = session;
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    String payload = message.getPayload();
                    if (payload.contains("\"event\":") || payload.equals("pong")) {
                        return;
                    }
                    if (OkxWebSocketClient.this.messageHandler != null) {
                        OkxWebSocketClient.this.messageHandler.accept(payload);
                    }
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    log.warn("OKX WebSocket connection closed: {}", status);
                    okxSession = null;
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    log.error("OKX WebSocket transport error", exception);
                }
            }, websocketUrl).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to connect to OKX WebSocket", e);
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void subscribe(String symbol) {
        if (okxSession == null || !okxSession.isOpen()) {
            log.warn("Cannot subscribe, OKX session is not active");
            return;
        }
        log.info("Subscribing to OKX order book for: {}", symbol);
        sendSubscriptionOp("subscribe", symbol);
    }

    public synchronized void unsubscribe(String symbol) {
        if (okxSession == null || !okxSession.isOpen()) {
            return;
        }
        log.info("Unsubscribing from OKX order book for: {}", symbol);
        sendSubscriptionOp("unsubscribe", symbol);
    }

    private void sendSubscriptionOp(String op, String symbol) {
        try {
            Map<String, Object> arg = Map.of("channel", "books", "instId", symbol);
            Map<String, Object> message = Map.of("op", op, "args", List.of(arg));
            String json = objectMapper.writeValueAsString(message);
            okxSession.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send subscription op to OKX", e);
        }
    }
}
