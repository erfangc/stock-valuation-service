package com.stockvaluationservice.crudder

import org.elasticsearch.client.RestHighLevelClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

class EntityManagerFactory(
    private val dynamoDbEnhancedClient: DynamoDbEnhancedClient,
    private val restHighLevelClient: RestHighLevelClient,
) {
    fun <T> create(tableName: String, clazz: Class<T>): EntityManager<T> {
        return EntityManager(
            dynamoDbEnhancedClient = dynamoDbEnhancedClient,
            restHighLevelClient = restHighLevelClient,
            clazz = clazz,
            tableName = tableName,
        )
    }
}