package com.stockvaluationservice.stockvaluationservice

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
class StockController {

    private val log = LoggerFactory.getLogger(StockController::class.java)

    @GetMapping("/stock-valuation-service")
    fun getStock(): StockQuote {
        log.info("Stock quote queried")
        return StockQuote(
            ticker = "AAPL",
            lastPrice = Random.nextDouble() * 100,
        )
    }

    @GetMapping
    fun get(): String {
        log.info("--- Health check pinged ---")
        return "Ok"
    }
}