package com.stockvaluationservice.stockvaluationservice

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StockController {
    @GetMapping("/stock-valuation-service")
    fun getStock(): StockQuote {
        return StockQuote(
            ticker = "AAPL",
            lastPrice = 100.02,
        )
    }

    @GetMapping
    fun get(): String {
        return "Ok"
    }
}