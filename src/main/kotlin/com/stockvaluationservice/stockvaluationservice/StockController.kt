package com.stockvaluationservice.stockvaluationservice

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StockController {

    private val log = LoggerFactory.getLogger(StockController::class.java)

    @GetMapping("/stock-valuation-service")
    fun getStock(): StockQuote {
        log.info("Stock quote queried")
        return StockQuote(
            ticker = "AAPL",
            lastPrice = 100.05,
        )
    }

    @GetMapping
    fun get(): String {
        log.info("--- Health check pinged ---")
        return "Ok"
    }
}