package com.assignment.market_data_service.controller;

import com.assignment.market_data_service.dto.MarketPairDto;
import com.assignment.market_data_service.service.MarketOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketApiController {

    private final MarketOverviewService marketOverviewService;

    public MarketApiController(MarketOverviewService marketOverviewService) {
        this.marketOverviewService = marketOverviewService;
    }

    @GetMapping("/overview")
    public Mono<ResponseEntity<List<MarketPairDto>>> getMarketOverview() {
        return marketOverviewService.getTopTradingPairs()
                .collectList()
                .map(ResponseEntity::ok);
    }
}
