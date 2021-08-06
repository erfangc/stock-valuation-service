package com.stockvaluationservice.stockvaluationservice.requestlogging.consumer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.concurrent.ArrayBlockingQueue
import javax.servlet.http.HttpServletRequest

@Service
class RequestQueueConsumer : Runnable {

    companion object {
        val requestQueue = ArrayBlockingQueue<RequestQueue>(2000)
    }

    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val log = LoggerFactory.getLogger(RequestQueueConsumer::class.java)

    override fun run() {
        while (true) {
            try {
                val queueElement = requestQueue.take()
                val request = queueElement.request as HttpServletRequest
                val requestId = queueElement.requestId
                val eventId = queueElement.eventId

                val bodyIsJson = request.contentType == MediaType.APPLICATION_JSON_VALUE
                val requestBody = if (bodyIsJson) {
                    objectMapper.readTree(request.inputStream)
                } else {
                    null
                }
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

}