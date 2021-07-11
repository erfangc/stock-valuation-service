package com.stockvaluationservice.stockvaluationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StockValuationServiceApplication

fun main(args: Array<String>) {
	runApplication<StockValuationServiceApplication>(*args)
}
