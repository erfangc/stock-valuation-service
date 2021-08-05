package com.stockvaluationservice.crudder

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class Permission(
    @get:DynamoDbPartitionKey
    var permissionId: String? = null,
    var subject: String? = null,
    var resourceId: String? = null,
    var resourceType: String? = null,
    var action: String? = null,
)