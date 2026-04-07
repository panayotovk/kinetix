package com.kinetix.risk.client

import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.PricePoint
import com.kinetix.risk.client.dtos.PricePointDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant

class HttpPriceServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : PriceServiceClient {

    private val logger = LoggerFactory.getLogger(HttpPriceServiceClient::class.java)

    @Serializable
    private data class UpstreamErrorBody(val code: String = "", val message: String = "")

    private val lenientJson = Json { ignoreUnknownKeys = true }

    private suspend fun errorResponseFor(response: HttpResponse): ClientResponse<Nothing> {
        val body = try {
            response.bodyAsText()
        } catch (_: Exception) {
            ""
        }
        val message = try {
            lenientJson.decodeFromString<UpstreamErrorBody>(body).message
        } catch (_: Exception) {
            body.ifBlank { response.status.description }
        }
        return if (response.status == HttpStatusCode.ServiceUnavailable) {
            ClientResponse.ServiceUnavailable()
        } else {
            ClientResponse.UpstreamError(response.status.value, message)
        }
    }

    override suspend fun getLatestPrice(instrumentId: InstrumentId): ClientResponse<PricePoint> = try {
        val response = httpClient.get("$baseUrl/api/v1/prices/${instrumentId.value}/latest")
        when {
            response.status == HttpStatusCode.NotFound -> ClientResponse.NotFound(response.status.value)
            response.status.isSuccess() -> ClientResponse.Success(response.body<PricePointDto>().toDomain())
            else -> errorResponseFor(response)
        }
    } catch (e: Exception) {
        logger.warn("Network error fetching latest price for {}", instrumentId.value, e)
        ClientResponse.NetworkError(e)
    }

    override suspend fun getPriceHistory(
        instrumentId: InstrumentId,
        from: Instant,
        to: Instant,
        interval: String?,
    ): ClientResponse<List<PricePoint>> = try {
        val response = httpClient.get("$baseUrl/api/v1/prices/${instrumentId.value}/history") {
            parameter("from", from.toString())
            parameter("to", to.toString())
            if (interval != null) parameter("interval", interval)
        }
        when {
            response.status == HttpStatusCode.NotFound -> ClientResponse.NotFound(response.status.value)
            response.status.isSuccess() -> ClientResponse.Success(response.body<List<PricePointDto>>().map { it.toDomain() })
            else -> errorResponseFor(response)
        }
    } catch (e: Exception) {
        logger.warn("Network error fetching price history for {}", instrumentId.value, e)
        ClientResponse.NetworkError(e)
    }
}
