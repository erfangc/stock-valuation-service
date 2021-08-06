package com.stockvaluationservice.stockvaluationservice.requestlogging.consumer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ArrayBlockingQueue
import javax.servlet.http.HttpServletResponse

@Service
class ResponseQueueConsumer : Runnable {

    private val log = LoggerFactory.getLogger(ResponseQueueConsumer::class.java)

    companion object {
        val responseQueue = ArrayBlockingQueue<ResponseQueue>(2000)
    }

    override fun run() {
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