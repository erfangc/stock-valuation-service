package com.stockvaluationservice.stockvaluationservice.requestlogging

import java.time.Instant
import java.util.*
import javax.servlet.ServletResponse

data class ResponseQueue(
    val eventId: String = UUID.randomUUID().toString(),
    val requestId: String,
    val response: ServletResponse,
    val timestamp: Instant = Instant.now(),
    val processingTimeMillis: Long,
    val user: String = "Unknown",
)