package com.stockvaluationservice.stockvaluationservice

import org.apache.http.HttpHost
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class InitSvc {

    private val port = 443
    private val log = LoggerFactory.getLogger(InitSvc::class.java)

    init {
        try {
            log.info("Attempting to test Elasticsearch domain")
            val client = RestHighLevelClient(
                RestClient.builder(
                    HttpHost(
                        "vpc-elasticsearch-domain-h2xqjpppxmpio7hp7hhpmnukne.us-east-1.es.amazonaws.com",
                        port,
                        "https"
                    )
                )
            )
            val id = UUID.randomUUID().toString()
            val request = IndexRequest("people")
                .id(id)
                .source(
                    """{ "firstName": "Erfang", "lastName": "Chen", "age": 20 }""",
                    XContentType.JSON
                )

            val indexResponse = client.index(request, RequestOptions.DEFAULT)

            if (indexResponse.result == DocWriteResponse.Result.CREATED) {
                log.info("Successfully created person document $id")
            } else if (indexResponse.result == DocWriteResponse.Result.UPDATED) {
                log.info("Successfully updated person document $id")
            }

            val shardInfo = indexResponse.shardInfo
            if (shardInfo.failed > 0) {
                shardInfo.failures.forEach { failure ->
                    log.error("Failure detected: index=${failure.index()} reason=${failure.reason()}")
                }
            }

            val getResponse = client.get(GetRequest("people").id(id), RequestOptions.DEFAULT)
            log.info("Getting the document back: ${getResponse.source}")

            client.close()
        } catch (ex: Exception) {
            log.error("Encountered error: ${ex.message}")
        }
    }

}