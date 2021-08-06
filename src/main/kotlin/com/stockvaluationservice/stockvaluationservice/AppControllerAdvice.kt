package com.stockvaluationservice.stockvaluationservice

import com.stockvaluationservice.stockvaluationservice.requestlogging.ThreadLocalRequestId
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class AppControllerAdvice(private val threadLocalRequestId: ThreadLocalRequestId) {

    private val log = LoggerFactory.getLogger(AppControllerAdvice::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiError> {
        val requestId = threadLocalRequestId.get()
        log.error("Error occurred requestId=$requestId", ex)
        return ResponseEntity.internalServerError()
            .body(ApiError(ex.message ?: "Internal server error occurred", requestId = requestId))
    }
}