package com.stockvaluationservice.stockvaluationservice.requestlogging

import java.time.Instant
import java.util.*
import javax.servlet.ServletRequest

data class RequestQueue(
    val eventId: String = UUID.randomUUID().toString(),
    val requestId: String,
    val request: ServletRequest,
    val timestamp: Instant = Instant.now(),
    val user: String = "Unknown",
)

