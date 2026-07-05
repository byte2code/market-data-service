package com.assignment.market_data_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/market")
    public String market() {
        return "market";
    }

    @GetMapping("/orderbook")
    public String orderBook(@org.springframework.web.bind.annotation.RequestParam String symbol, org.springframework.ui.Model model) {
        model.addAttribute("symbol", symbol);
        return "orderbook";
    }
}
