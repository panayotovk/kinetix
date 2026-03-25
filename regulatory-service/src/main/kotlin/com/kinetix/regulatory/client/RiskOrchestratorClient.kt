package com.kinetix.regulatory.client

import com.kinetix.regulatory.dto.FrtbResultResponse
import com.kinetix.regulatory.historical.dto.ReplayResultResponse
import com.kinetix.regulatory.stress.dto.ReverseStressResultResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
private data class HistoricalReplayRequestBody(
    val instrumentReturns: List<InstrumentDailyReturnsBody>,
    val windowStart: String?,
    val windowEnd: String?,
)

@Serializable
private data class InstrumentDailyReturnsBody(
    val instrumentId: String,
    val dailyReturns: List<Double>,
)

@Serializable
private data class ReverseStressRequestBody(
    val targetLoss: Double,
    val maxShock: Double,
)

@Serializable
private data class StressTestRequestBody(
    val scenarioName: String,
    val priceShocks: Map<String, Double>? = null,
)

class RiskOrchestratorClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun calculateFrtb(bookId: String): FrtbResultResponse {
        val response = httpClient.post("$baseUrl/api/v1/regulatory/frtb/$bookId") {
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }

    suspend fun runHistoricalReplay(
        bookId: String,
        instrumentReturns: Map<String, List<Double>>,
        windowStart: String?,
        windowEnd: String?,
    ): ReplayResultResponse {
        val body = HistoricalReplayRequestBody(
            instrumentReturns = instrumentReturns.map { (id, returns) ->
                InstrumentDailyReturnsBody(instrumentId = id, dailyReturns = returns)
            },
            windowStart = windowStart,
            windowEnd = windowEnd,
        )
        val response = httpClient.post("$baseUrl/api/v1/risk/stress/$bookId/historical-replay") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.body()
    }

    suspend fun runReverseStress(
        bookId: String,
        targetLoss: Double,
        maxShock: Double,
    ): ReverseStressResultResponse {
        val body = ReverseStressRequestBody(targetLoss = targetLoss, maxShock = maxShock)
        val response = httpClient.post("$baseUrl/api/v1/risk/stress/$bookId/reverse") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.body()
    }

    suspend fun runStressTest(
        bookId: String,
        scenarioName: String,
        priceShocks: Map<String, Double>,
    ): StressTestResultDto {
        val body = StressTestRequestBody(scenarioName = scenarioName, priceShocks = priceShocks)
        val response = httpClient.post("$baseUrl/api/v1/risk/stress/$bookId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.body()
    }
}
