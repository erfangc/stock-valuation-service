package com.stockvaluationservice.stockvaluationservice

import java.time.Instant

data class StockQuote(
    val ticker: String,
    val lastPrice: Double,
    val timestamp: Instant = Instant.now(),
)