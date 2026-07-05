package com.assignment.market_data_service.service;

import com.assignment.market_data_service.dto.OrderBookDto;
import reactor.core.publisher.Flux;

public interface OrderBookService {
    Flux<OrderBookDto> subscribeOrderBook(String symbol);
}
