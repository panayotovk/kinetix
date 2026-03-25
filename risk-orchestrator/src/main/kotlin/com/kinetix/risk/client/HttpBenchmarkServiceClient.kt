package com.kinetix.risk.client

import com.kinetix.risk.client.dtos.BenchmarkDetailDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.time.LocalDate

class HttpBenchmarkServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : BenchmarkServiceClient {

    override suspend fun getBenchmarkDetail(
        benchmarkId: String,
        asOfDate: LocalDate,
    ): ClientResponse<BenchmarkDetailDto> {
        val response = httpClient.get("$baseUrl/api/v1/benchmarks/$benchmarkId?asOfDate=$asOfDate")
        if (response.status == HttpStatusCode.NotFound) return ClientResponse.NotFound(response.status.value)
        val dto: BenchmarkDetailDto = response.body()
        return ClientResponse.Success(dto)
    }
}
