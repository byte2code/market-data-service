package com.assignment.market_data_service.service;

import com.assignment.market_data_service.client.OkxWebSocketClient;
import com.assignment.market_data_service.dto.OrderBookDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class OrderBookServiceImpl implements OrderBookService {

    private static final Logger log = LoggerFactory.getLogger(OrderBookServiceImpl.class);
    private final OkxWebSocketClient okxWebSocketClient;
    private final ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> symbolSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> sessionSymbols = new ConcurrentHashMap<>();

    public OrderBookServiceImpl(OkxWebSocketClient okxWebSocketClient, ObjectMapper objectMapper) {
        this.okxWebSocketClient = okxWebSocketClient;
        this.objectMapper = objectMapper;
        this.okxWebSocketClient.connect(new OkxWebSocketClient.OkxListener() {
            @Override
            public void onConnected() {
                log.info("OKX WebSocket connected. Re-subscribing to active symbols: {}", symbolSessions.keySet());
                for (String symbol : symbolSessions.keySet()) {
                    okxWebSocketClient.subscribe(symbol);
                }
            }

            @Override
            public void onMessage(String payload) {
                processOkxMessage(payload);
            }
        });
    }

    @Override
    public Flux<OrderBookDto> subscribeOrderBook(String symbol) {
        return Flux.empty();
    }

    public void handleClientSubscribe(WebSocketSession session, String symbol) {
        log.info("Client session {} subscribing to {}", session.getId(), symbol);
        
        symbolSessions.computeIfAbsent(symbol, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionSymbols.computeIfAbsent(session.getId(), k -> new CopyOnWriteArraySet<>()).add(symbol);

        if (symbolSessions.get(symbol).size() == 1) {
            okxWebSocketClient.subscribe(symbol);
        }
    }

    public void handleClientUnsubscribe(WebSocketSession session, String symbol) {
        log.info("Client session {} unsubscribing from {}", session.getId(), symbol);
        
        CopyOnWriteArraySet<WebSocketSession> sessions = symbolSessions.get(symbol);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                symbolSessions.remove(symbol);
                okxWebSocketClient.unsubscribe(symbol);
            }
        }

        CopyOnWriteArraySet<String> symbols = sessionSymbols.get(session.getId());
        if (symbols != null) {
            symbols.remove(symbol);
            if (symbols.isEmpty()) {
                sessionSymbols.remove(session.getId());
            }
        }
    }

    public void handleSessionDisconnect(WebSocketSession session) {
        log.info("Session disconnected, cleaning up: {}", session.getId());
        CopyOnWriteArraySet<String> symbols = sessionSymbols.remove(session.getId());
        if (symbols != null) {
            for (String symbol : symbols) {
                CopyOnWriteArraySet<WebSocketSession> sessions = symbolSessions.get(symbol);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        symbolSessions.remove(symbol);
                        okxWebSocketClient.unsubscribe(symbol);
                    }
                }
            }
        }
    }

    private void processOkxMessage(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode argNode = root.path("arg");
            String symbol = argNode.path("instId").asText();
            if (symbol == null || symbol.isEmpty()) {
                return;
            }

            CopyOnWriteArraySet<WebSocketSession> sessions = symbolSessions.get(symbol);
            if (sessions == null || sessions.isEmpty()) {
                return;
            }

            JsonNode dataNode = root.path("data");
            if (dataNode.isArray() && dataNode.size() > 0) {
                JsonNode firstData = dataNode.get(0);
                OrderBookDto dto = parseOrderBookData(symbol, firstData);
                
                String messageJson = objectMapper.writeValueAsString(dto);
                TextMessage textMessage = new TextMessage(messageJson);

                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            log.error("Failed to send message to client session " + session.getId(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing OKX WebSocket message", e);
        }
    }

    private OrderBookDto parseOrderBookData(String symbol, JsonNode data) {
        OrderBookDto dto = new OrderBookDto();
        dto.setSymbol(symbol);

        List<OrderBookDto.PriceLevel> bids = new ArrayList<>();
        JsonNode bidsNode = data.path("bids");
        if (bidsNode.isArray()) {
            for (int i = 0; i < Math.min(bidsNode.size(), 15); i++) {
                JsonNode bid = bidsNode.get(i);
                OrderBookDto.PriceLevel pl = new OrderBookDto.PriceLevel();
                pl.setPrice(bid.get(0).asText());
                pl.setSize(bid.get(1).asText());
                bids.add(pl);
            }
        }
        dto.setBids(bids);

        List<OrderBookDto.PriceLevel> asks = new ArrayList<>();
        JsonNode asksNode = data.path("asks");
        if (asksNode.isArray()) {
            for (int i = 0; i < Math.min(asksNode.size(), 15); i++) {
                JsonNode ask = asksNode.get(i);
                OrderBookDto.PriceLevel pl = new OrderBookDto.PriceLevel();
                pl.setPrice(ask.get(0).asText());
                pl.setSize(ask.get(1).asText());
                asks.add(pl);
            }
        }
        dto.setAsks(asks);

        return dto;
    }
}
