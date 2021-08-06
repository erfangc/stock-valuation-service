package com.stockvaluationservice.stockvaluationservice.requestlogging

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * TODO
 * 1 - How to serialize properly in DynamoDB?
 * 2 - Role mapping ... ?
 * 3 - Would CachedRequest crash on multipart file upload?
 */
@Component
class RequestLoggingFilter(
    private val threadLocalRequestId: ThreadLocalRequestId
) : Filter {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
    private val requestQueue = ArrayBlockingQueue<RequestQueue>(2000)
    private val responseQueue = ArrayBlockingQueue<ResponseQueue>(2000)

    private val executor = Executors.newFixedThreadPool(2)
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                executor.shutdown()
            }
        )
    }

    @PostConstruct
    fun init0() {
        executor.submit {
            while (true) {
                try {
                    val queueElement = requestQueue.take()
                    val request = queueElement.request as HttpServletRequest
                    val requestId = queueElement.requestId
                    val eventId = queueElement.eventId

                    val bodyIsJson = request.contentType == APPLICATION_JSON_VALUE
                    val requestBody = if (bodyIsJson)
                        objectMapper.readTree(request.inputStream)
                    else
                        null

                    log.info(
                        "Processing request " +
                                "requestURI=${request.requestURI} " +
                                "method=${request.method} " +
                                "requestId=${requestId} " +
                                "eventId=${eventId} " +
                                "requestBody=$requestBody "
                    )
                } catch (ex: Exception) {
                    log.error("Cannot write request", ex)
                }
            }
        }
        executor.submit {
            while (true) {
                try {
                    val queueElement = responseQueue.take()
                    val response = queueElement.response as HttpServletResponse
                    val requestId = queueElement.requestId
                    val processingTimeMillis = queueElement.processingTimeMillis
                    val eventId = queueElement.eventId
                    val status = response.status

                    log.info(
                        "Processed request " +
                                "requestId=${requestId} eventId=$eventId status=$status processingTimeMillis=$processingTimeMillis"
                    )
                } catch (ex: Exception) {
                    log.error("Cannot write request", ex)
                }
            }
        }
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        threadLocalRequestId.set()
        val requestId = threadLocalRequestId.get()

        val cachedRequest = CachedBodyHttpServletRequest(request as HttpServletRequest)
        requestQueue.put(RequestQueue(requestId = requestId, request = cachedRequest))

        val start = System.currentTimeMillis()
        chain.doFilter(cachedRequest, response)
        val stop = System.currentTimeMillis()
        val processingTimeMillis = stop - start

        responseQueue.put(
            ResponseQueue(
                requestId = requestId,
                response = response,
                processingTimeMillis = processingTimeMillis
            )
        )
    }

}