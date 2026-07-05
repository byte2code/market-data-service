package com.assignment.market_data_service.service;

import com.assignment.market_data_service.dto.MarketPairDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MarketOverviewServiceImpl implements MarketOverviewService {

    @Override
    public Flux<MarketPairDto> getTopTradingPairs() {
        return Flux.empty();
    }
}
