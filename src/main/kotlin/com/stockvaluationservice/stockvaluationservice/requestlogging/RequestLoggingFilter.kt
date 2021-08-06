package com.stockvaluationservice.stockvaluationservice.requestlogging

import com.stockvaluationservice.stockvaluationservice.requestlogging.consumer.RequestQueue
import com.stockvaluationservice.stockvaluationservice.requestlogging.consumer.RequestQueueConsumer
import com.stockvaluationservice.stockvaluationservice.requestlogging.consumer.RequestQueueConsumer.Companion.requestQueue
import com.stockvaluationservice.stockvaluationservice.requestlogging.consumer.ResponseQueue
import com.stockvaluationservice.stockvaluationservice.requestlogging.consumer.ResponseQueueConsumer
import com.stockvaluationservice.stockvaluationservice.requestlogging.consumer.ResponseQueueConsumer.Companion.responseQueue
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

/**
 * This filter wraps around the execution of HTTP requests and perform the following tasks:
 *
 * 1. Generate the requestId for the current request, which can be accessed from all Spring components such as the
 * controller advice
 *
 * 2. Convert the [ServletRequest] into a [CachedBodyHttpServletRequest] by prefetching all the bytes from the request
 * body and storing them inside a reusable byte array. This enables us to repeatedly access the request body input stream
 * without encountering a "stream is closed" error
 *
 * 3. Push the request object into a queue to be logged permanently into DynamoDB/Elasticsearch for auditing purposes
 */
@Component
class RequestLoggingFilter(
    private val threadLocalRequestId: ThreadLocalRequestId,
    requestQueueConsumer: RequestQueueConsumer,
    responseQueueConsumer: ResponseQueueConsumer,
) : Filter {

    private val executor = Executors.newFixedThreadPool(2)

    /**
     * The initialization block that starts the queue consumers
     */
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread { executor.shutdown() }
        )
        executor.submit(requestQueueConsumer)
        executor.submit(responseQueueConsumer)
    }

    /**
     * Perform the logic of the filter see detail of what [doFilter] does in [RequestLoggingFilter]
     *
     * @see [RequestLoggingFilter]
     * @param request
     * @param response
     * @param chain
     */
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {

        /*
        Sets the requestId for the entire request, any Spring Boot service
        that calls threadLocalRequestId.get() from this point onwards will
        get the same requestId as is being set here (as long as the caller is from the same thread - which should
        be the case unless you are using an executor)
         */
        threadLocalRequestId.set()
        val requestId = threadLocalRequestId.get()

        val cachedRequest = CachedBodyHttpServletRequest(request as HttpServletRequest)

        // logging of the request to DynamoDB/Elasticsearch occurs asynchronously to avoid blocking
        // this HTTP handling thread
        requestQueue.put(RequestQueue(requestId = requestId, request = cachedRequest))

        val start = System.currentTimeMillis()
        chain.doFilter(cachedRequest, response)
        val stop = System.currentTimeMillis()
        val processingTimeMillis = stop - start

        // logging of the response to DynamoDB/Elasticsearch occurs asynchronously to avoid blocking
        // this HTTP handling thread
        responseQueue.put(
            ResponseQueue(
                requestId = requestId,
                response = response,
                processingTimeMillis = processingTimeMillis
            )
        )
    }

}