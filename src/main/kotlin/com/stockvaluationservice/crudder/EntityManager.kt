package com.stockvaluationservice.crudder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.rest.RestStatus
import org.slf4j.LoggerFactory
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

class EntityManager<T>(
    dynamoDbEnhancedClient: DynamoDbEnhancedClient,
    private val restHighLevelClient: RestHighLevelClient,
    private val clazz: Class<T>,
    private val tableName: String,
) {

    private val log = LoggerFactory.getLogger(EntityManager::class.java)
    private val table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromClass(clazz))

    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun table() = table

    fun delete(id: String) {
        table.deleteItem(Key.builder().partitionValue(id).build())
        restHighLevelClient.delete(DeleteRequest(tableName).id(id), RequestOptions.DEFAULT)
    }

    fun create(entity: T, permissions: List<Permission> = emptyList()) {
        val id = resolveEntityId(entity)
        /*
        DynamoDB PutItem operation
         */
        table.putItem(entity)

        /*
        Attach permissions to the Elasticsearch document
         */
        val jsonNode = jsonifyWithPermissionAttachment(entity, permissions)

        /*
        Index the document
         */
        indexDocument(id, jsonNode.toString())
    }

    fun grantPermission(permission: Permission) {
        val id = permission.resourceId ?: error("resourceId cannot be blank")
        val getResponse = getDocument(id)
        val jsonNode = getResponse.asJson()
        val permissions = jsonNode.withArray<ArrayNode>("permissions")
        permissions.add("${permission.subject}:${permission.action}")
        updateDocument(id= id, source = jsonNode.toString(), getResponse = getResponse)
    }

    fun revokePermission(permission: Permission) {
        val id = permission.resourceId ?: error("resourceId cannot be blank")
        val getResponse = getDocument(id)
        val jsonNode = getResponse.asJson()
        val permissions = jsonNode.withArray<ArrayNode>("permissions")
        permissions.removeElement("${permission.subject}:${permission.action}")
        updateDocument(id= id, source = jsonNode.toString(), getResponse = getResponse)
    }

    fun update(entity: T) {
        val id = resolveEntityId(entity)
        val getResponse = getDocument(id)

        val newNode = objectMapper.valueToTree<ObjectNode>(entity)
        newNode.copyArrayProperty(
            from = getResponse.asJson(),
            propertyName = "permissions",
        )

        updateDocument(
            id = id,
            source = newNode.toString(),
            getResponse = getResponse
        )
    }

    private fun GetResponse.asJson() = objectMapper.readTree(this.sourceAsString)

    private fun ArrayNode.removeElement(element: String) {
        this.forEachIndexed { index, node ->
            if (node.isTextual && node.textValue() == element) {
                this.remove(index)
            }
        }
    }

    private fun JsonNode.copyArrayProperty(
        from: JsonNode,
        propertyName: String,
    ) {
        if (!from.isObject || !this.isObject) {
            return
        }
        val arrayNode = (from as ObjectNode).withArray(propertyName)
        // copy permissions from the old object to the new
        val permissions = (this as ObjectNode).withArray(propertyName)
        arrayNode.forEach { permission ->
            permissions.add(permission)
        }
    }

    private fun jsonifyWithPermissionAttachment(entity: T, permissions: List<Permission>): ObjectNode {
        val objectNode = objectMapper.valueToTree<ObjectNode>(entity)
        val arrayNode = objectNode.withArray("permissions")
        permissions.forEach { permission ->
            arrayNode.add("${permission.subject}:${permission.action}")
        }
        return objectNode
    }

    private fun getDocument(id: String): GetResponse {
        val getResponse = restHighLevelClient.get(GetRequest(tableName).id(id), RequestOptions.DEFAULT)
        return if (getResponse.isExists) {
            getResponse
        } else {
            error("document with id $id cannot be found in index $tableName")
        }
    }

    private fun updateDocument(
        id: String,
        source: String,
        getResponse: GetResponse
    ) {
        try {
            val indexResponse = restHighLevelClient.index(
                IndexRequest(tableName)
                    .id(id)
                    .source(source, XContentType.JSON)
                    .setIfSeqNo(getResponse.seqNo)
                    .setIfPrimaryTerm(getResponse.primaryTerm),
                RequestOptions.DEFAULT,
            )
            val shardInfo = indexResponse.shardInfo
            if (shardInfo.total != shardInfo.successful) {
                log.info("Some shards failed to index tableName=$tableName documentId=$id")
            }
            if (shardInfo.failed > 0) {
                for (failure in shardInfo.failures) {
                    log.error("Failure reason=${failure.reason()} index=${failure.index()} fullShardId=${failure.fullShardId()} nodeId=${failure.nodeId()} primary=${failure.primary()}")
                }
            }
        } catch (ex: ElasticsearchException) {
            if (ex.status() == RestStatus.CONFLICT) {
                log.error("The document $id in index $tableName has been modified by another process")
            } else {
                log.error("Elasticsearch server-side exception occurred", ex)
            }
        } catch (ex: Exception) {
            log.error("Exception occurred", ex)
        }
    }

    private fun indexDocument(id: String, source: String) {
        try {
            val indexResponse = restHighLevelClient.index(
                IndexRequest(tableName)
                    .id(id)
                    .source(source, XContentType.JSON),
                RequestOptions.DEFAULT,
            )
            val shardInfo = indexResponse.shardInfo
            if (shardInfo.total != shardInfo.successful) {
                log.info("Some shards failed to index tableName=$tableName documentId=$id")
            }
            if (shardInfo.failed > 0) {
                for (failure in shardInfo.failures) {
                    log.error("Failure reason=${failure.reason()} index=${failure.index()} fullShardId=${failure.fullShardId()} nodeId=${failure.nodeId()} primary=${failure.primary()}")
                }
            }
        } catch (ex: ElasticsearchException) {
            log.error("Elasticsearch server-side exception occurred", ex)
        } catch (ex: Exception) {
            log.error("Exception occurred", ex)
        }
    }

    private fun resolveEntityId(entity: T): String {
        val primaryKeyMethod = clazz.methods.find { method ->
            method.annotations.any { annotation -> annotation.annotationClass == DynamoDbPartitionKey::class }
        }
        return primaryKeyMethod?.invoke(entity)?.toString()
            ?: error(
                "Unable to determine entity id of type ${clazz.name}. " +
                        "Please annotate the id property in your Kotlin data class with @get:DynamoDbPartitionKey"
            )
    }

}
