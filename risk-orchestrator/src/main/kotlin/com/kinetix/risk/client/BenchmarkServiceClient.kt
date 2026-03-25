package com.kinetix.risk.client

import com.kinetix.risk.client.dtos.BenchmarkDetailDto
import java.time.LocalDate

interface BenchmarkServiceClient {
    suspend fun getBenchmarkDetail(benchmarkId: String, asOfDate: LocalDate): ClientResponse<BenchmarkDetailDto>
}
