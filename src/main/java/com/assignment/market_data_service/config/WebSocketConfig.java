package com.assignment.market_data_service.config;

import com.assignment.market_data_service.websocket.ClientWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClientWebSocketHandler clientWebSocketHandler;

    public WebSocketConfig(ClientWebSocketHandler clientWebSocketHandler) {
        this.clientWebSocketHandler = clientWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientWebSocketHandler, "/ws/orderbook")
                .setAllowedOrigins("http://localhost:8080", "http://127.0.0.1:8080", "https://localhost:8080", "https://127.0.0.1:8080");
    }
}
