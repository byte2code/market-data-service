package com.assignment.market_data_service.service;

import com.assignment.market_data_service.dto.OrderBookDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class OrderBookServiceImpl implements OrderBookService {

    @Override
    public Flux<OrderBookDto> subscribeOrderBook(String symbol) {
        return Flux.empty();
    }
}
