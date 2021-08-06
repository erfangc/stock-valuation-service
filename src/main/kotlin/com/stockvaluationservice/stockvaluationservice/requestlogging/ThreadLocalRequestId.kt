package com.stockvaluationservice.stockvaluationservice.requestlogging

import org.springframework.stereotype.Service
import java.util.*

/**
 * ## Background
 *
 * Inject this service into any Spring component to get the requestId of the current HTTP request
 * being processed
 *
 * This service uses a ThreadLocal to ensure if different HTTP processing threads called the [set] and [get] the effect
 * is only felt in the Thread processing the current HTTP request
 *
 * ## Lifecycle
 *
 * The requestId is set initially in [RequestLoggingFilter]
 * any Spring components that interact with any given HTTP request from that point onwards can access the requestId
 * by simply calling [get]
 *
 * ## Example
 *
 * ```kotlin
 * @Service
 * class MyService(private val threadLocalRequestId) {
 *   fun myMethod() {
 *     // Each thread magically gets the correct requestId
 *     val requestId = threadLocalRequestId.get()
 *   }
 * }
 * ```
 *
 */
@Service
class ThreadLocalRequestId {

    private val threadLocal = ThreadLocal<String>()

    fun set() = threadLocal.set(UUID.randomUUID().toString())

    fun get() = threadLocal.get()

}