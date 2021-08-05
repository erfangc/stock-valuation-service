package com.stockvaluationservice.crudder
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.util.*
import kotlin.system.exitProcess

fun main() {

    val restHighLevelClient = RestHighLevelClient(
        RestClient.builder(HttpHost("localhost", 9200, "http"))
    )
    val dynamoDbClient = DynamoDbClient.builder().build()
    val dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build()
    val factory = EntityManagerFactory(dynamoDbEnhancedClient, restHighLevelClient)

    val entityManager = factory.create("sample", Person::class.java)

    entityManager.create(
        Person(id = UUID.randomUUID().toString(), firstName = "EE", lastName = "vvv", age = 213),
        permissions = listOf(
            Permission(subject = "n00b", action = "View"),
            Permission(subject = "l33ts", action = "View"),
        )
    )

    entityManager.create(
        Person(id = UUID.randomUUID().toString(), firstName = "EE", lastName = "vvv", age = 213),
        permissions = listOf(
            Permission(subject = "l33ts", action = "View"),
        )
    )

    exitProcess(0)
}