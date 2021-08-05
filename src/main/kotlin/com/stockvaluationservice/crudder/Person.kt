package com.stockvaluationservice.crudder

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class Person(
    @get:DynamoDbPartitionKey
    var id: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var age: Int? = null,
)