package com.assignment.market_data_service.exception;

public class MarketDataException extends RuntimeException {
    public MarketDataException(String message) {
        super(message);
    }

    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
