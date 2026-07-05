package com.assignment.market_data_service.controller;

import com.assignment.market_data_service.client.OkxWebSocketClient;
import com.assignment.market_data_service.dto.MarketPairDto;
import com.assignment.market_data_service.service.MarketOverviewService;
import com.assignment.market_data_service.service.OrderBookServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@RestController
@RequestMapping("/api/market")
public class MarketApiController {

    private final MarketOverviewService marketOverviewService;
    private final OkxWebSocketClient okxWebSocketClient;
    private final OrderBookServiceImpl orderBookService;

    public MarketApiController(MarketOverviewService marketOverviewService,
                               OkxWebSocketClient okxWebSocketClient,
                               OrderBookServiceImpl orderBookService) {
        this.marketOverviewService = marketOverviewService;
        this.okxWebSocketClient = okxWebSocketClient;
        this.orderBookService = orderBookService;
    }

    @GetMapping("/overview")
    public Mono<ResponseEntity<List<MarketPairDto>>> getMarketOverview() {
        return marketOverviewService.getTopTradingPairs()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("okx_connected", okxWebSocketClient.isConnected());
        
        List<String> symbols = new ArrayList<>(orderBookService.getSymbolSessions().keySet());
        info.put("subscribed_symbols", symbols);
        
        Map<String, Integer> sessionsCount = new HashMap<>();
        orderBookService.getSymbolSessions().forEach((k, v) -> sessionsCount.put(k, v.size()));
        info.put("symbol_sessions_count", sessionsCount);
        
        return ResponseEntity.ok(info);
    }
}
