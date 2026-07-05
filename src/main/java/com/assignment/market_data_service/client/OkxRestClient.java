package com.assignment.market_data_service.client;

import com.assignment.market_data_service.dto.MarketPairDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class OkxRestClient {

    public Flux<MarketPairDto> fetchTopTradingPairs() {
        return Flux.empty();
    }
}
