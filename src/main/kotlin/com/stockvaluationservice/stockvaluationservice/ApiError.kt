package com.stockvaluationservice.stockvaluationservice

data class ApiError(
    val message: String,
    val requestId: String,
)