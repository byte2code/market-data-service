package com.assignment.market_data_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MarketController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/market")
    public String market() {
        return "market";
    }

    @GetMapping("/orderbook")
    public String orderBook() {
        return "orderbook";
    }
}
