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
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class OkxWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(OkxWebSocketClient.class);
    private final String websocketUrl;
    private final ObjectMapper objectMapper;
    private final WebSocketClient client;
    private WebSocketSession okxSession;
    private OkxListener listener;
    private final ScheduledExecutorService heartbeatScheduler;

    public interface OkxListener {
        void onConnected();
        void onMessage(String payload);
    }

    public OkxWebSocketClient(@Value("${okx.websocket-url}") String websocketUrl, ObjectMapper objectMapper) {
        this.websocketUrl = websocketUrl;
        this.objectMapper = objectMapper;
        
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(10 * 1024 * 1024); // 10MB
        container.setDefaultMaxBinaryMessageBufferSize(10 * 1024 * 1024);
        this.client = new StandardWebSocketClient(container);
        
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        startHeartbeat();
    }

    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (okxSession != null && okxSession.isOpen()) {
                    try {
                        log.debug("Sending heartbeat ping to OKX");
                        okxSession.sendMessage(new TextMessage("ping"));
                    } catch (IOException e) {
                        log.error("Failed to send heartbeat ping to OKX", e);
                    }
                }
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    public synchronized void connect(OkxListener listener) {
        this.listener = listener;
        if (okxSession != null && okxSession.isOpen()) {
            return;
        }
        log.info("Connecting asynchronously to OKX WebSocket API: {}", websocketUrl);
        CompletableFuture.runAsync(() -> {
            try {
                client.execute(new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) {
                        log.info("OKX WebSocket connection established");
                        okxSession = session;
                        if (OkxWebSocketClient.this.listener != null) {
                            OkxWebSocketClient.this.listener.onConnected();
                        }
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        String payload = message.getPayload();
                        if (payload.contains("\"event\":") || payload.equals("pong")) {
                            return;
                        }
                        if (OkxWebSocketClient.this.listener != null) {
                            OkxWebSocketClient.this.listener.onMessage(payload);
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
            } catch (Exception e) {
                log.error("Failed to connect to OKX WebSocket", e);
            }
        });
    }

    public synchronized void subscribe(String symbol) {
        if (okxSession == null || !okxSession.isOpen()) {
            log.warn("OKX WebSocket session is not active. Attempting to connect...");
            connect(this.listener);
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

    public synchronized boolean isConnected() {
        return okxSession != null && okxSession.isOpen();
    }
}
