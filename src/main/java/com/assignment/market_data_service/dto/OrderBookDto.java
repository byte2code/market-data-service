package com.assignment.market_data_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderBookDto {
    private String symbol;
    private List<PriceLevel> bids;
    private List<PriceLevel> asks;

    @Data
    public static class PriceLevel {
        private String price;
        private String size;
    }
}
