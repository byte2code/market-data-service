package com.assignment.market_data_service.service;

import org.springframework.web.socket.WebSocketSession;

public interface OrderBookService {
    void handleClientSubscribe(WebSocketSession session, String symbol);
    void handleClientUnsubscribe(WebSocketSession session, String symbol);
    void handleSessionDisconnect(WebSocketSession session);
}
