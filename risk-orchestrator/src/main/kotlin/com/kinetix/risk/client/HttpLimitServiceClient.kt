package com.kinetix.risk.client

import com.kinetix.risk.client.dtos.LimitDefinitionDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class HttpLimitServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : LimitServiceClient {

    override suspend fun getLimits(): ClientResponse<List<LimitDefinitionDto>> {
        val response = httpClient.get("$baseUrl/api/v1/limits")
        if (response.status == HttpStatusCode.NotFound) return ClientResponse.NotFound(response.status.value)
        val dtos: List<LimitDefinitionDto> = response.body()
        return ClientResponse.Success(dtos)
    }
}
