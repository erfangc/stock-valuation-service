package com.stockvaluationservice.stockvaluationservice.requestlogging

import org.springframework.stereotype.Service
import java.util.*

@Service
class ThreadLocalRequestId {
    private val threadLocal = ThreadLocal<String>()
    fun set() = threadLocal.set(UUID.randomUUID().toString())
    fun get() = threadLocal.get()
}