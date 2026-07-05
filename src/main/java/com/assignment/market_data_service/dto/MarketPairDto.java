package com.assignment.market_data_service.dto;

import lombok.Data;

@Data
public class MarketPairDto {
    private String symbol;
    private String lastPrice;
    private String changePercent24h;
    private String volume24h;
}
