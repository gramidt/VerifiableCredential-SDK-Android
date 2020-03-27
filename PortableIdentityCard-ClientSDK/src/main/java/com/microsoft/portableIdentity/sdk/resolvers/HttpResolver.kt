package com.microsoft.portableIdentity.sdk.resolvers

import com.microsoft.portableIdentity.sdk.identifier.document.IdentifierDocument
import com.microsoft.portableIdentity.sdk.utilities.*
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.serialization.Serializable

/**
 * Fetches Identifier Documents from remote resolvers over http.
 * @class
 * @implements IResolver
 * @param url of the remote resolver.
 */
class HttpResolver(private val baseUrl : String, logger: ILogger): IResolver(logger) {

    @Serializable
    data class ResolverMetadata(val driverId: String?, val driver: String?, val retrieved: String?, val duration: String?)

    @Serializable
    data class ResolverResult(val document: com.microsoft.portableIdentity.sdk.identifier.deprecated.document.IdentifierDocument, val resolverMetadata: ResolverMetadata)

    /**
     * Sends a fetch request to the resolver URL
     * to resolver specified Identifier
     * @param identifier to resolve
     */
    override suspend fun resolveDocument(identifier: String): com.microsoft.portableIdentity.sdk.identifier.deprecated.document.IdentifierDocument {
        val client = getHttpClient()
        val response = client.get<String>() {
            url("$baseUrl/$identifier ")
        }
        client.close()
        println("GOT $response")
        val result = Serializer.parse(ResolverResult.serializer(), response)
        println("resolved ${result.document.id} with metadata ${result.resolverMetadata}")
        return result.document
    }

    override suspend fun resolveDocument(identifier: String, initialValues: String): com.microsoft.portableIdentity.sdk.identifier.document.IdentifierDocument {
        val client = getHttpClient()
        val response = client.get<String>() {
            url("$baseUrl/$identifier?-ion-initial-state=$initialValues ")
        }
        client.close()
        println("GOT $response")
        val result = Serializer.parse(IdentifierDocument.serializer(), response)
        return result
    }
}