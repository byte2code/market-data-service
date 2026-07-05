package com.assignment.market_data_service.service;

import com.assignment.market_data_service.dto.MarketPairDto;
import reactor.core.publisher.Flux;

public interface MarketOverviewService {
    Flux<MarketPairDto> getTopTradingPairs();
}
