package com.kinetix.regulatory.historical

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.historical.dto.PositionReplayImpact
import com.kinetix.regulatory.historical.dto.ReplayResultResponse
import com.kinetix.regulatory.module
import com.kinetix.regulatory.persistence.FrtbCalculationRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk

class HistoricalReplayRouteTest : FunSpec({

    val repository = mockk<HistoricalScenarioRepository>()
    val riskClient = mockk<RiskOrchestratorClient>()

    test("POST /api/v1/historical-periods/{periodId}/replay returns replay result for known period") {
        val period = HistoricalScenarioPeriod(
            periodId = "GFC_OCT_2008",
            name = "GFC_OCT_2008",
            description = "Global Financial Crisis",
            startDate = "2008-09-15",
            endDate = "2009-03-15",
            assetClassFocus = "EQUITY",
            severityLabel = "SEVERE",
        )
        coEvery { repository.findPeriodById("GFC_OCT_2008") } returns period
        coEvery { repository.findAllReturns("GFC_OCT_2008") } returns emptyList()
        coEvery {
            riskClient.runHistoricalReplay(
                bookId = "BOOK-1",
                instrumentReturns = any(),
                windowStart = "2008-09-15",
                windowEnd = "2009-03-15",
            )
        } returns ReplayResultResponse(
            periodId = "GFC_OCT_2008",
            scenarioName = "GFC_OCT_2008",
            bookId = "BOOK-1",
            totalPnlImpact = "-25000.00",
            positionImpacts = listOf(
                PositionReplayImpact("AAPL", "EQUITY", "100000.00", "-25000.00", listOf("-3500.00", "-5500.00"), false),
            ),
            windowStart = "2008-09-15",
            windowEnd = "2009-03-15",
            calculatedAt = "2026-03-25T12:00:00Z",
        )

        testApplication {
            application {
                module(
                    repository = mockk<FrtbCalculationRepository>(),
                    client = riskClient,
                    historicalScenarioRepository = repository,
                )
            }
            val response = client.post("/api/v1/historical-periods/GFC_OCT_2008/replay") {
                contentType(ContentType.Application.Json)
                setBody("""{"bookId":"BOOK-1"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            val body = response.bodyAsText()
            body shouldContain "\"periodId\":\"GFC_OCT_2008\""
            body shouldContain "\"bookId\":\"BOOK-1\""
            body shouldContain "\"totalPnlImpact\":\"-25000.00\""
            body shouldContain "\"proxyUsed\":false"
        }
    }

    test("POST /api/v1/historical-periods/{periodId}/replay returns 404 for unknown period") {
        coEvery { repository.findPeriodById("UNKNOWN_PERIOD") } returns null

        testApplication {
            application {
                module(
                    repository = mockk<FrtbCalculationRepository>(),
                    client = riskClient,
                    historicalScenarioRepository = repository,
                )
            }
            val response = client.post("/api/v1/historical-periods/UNKNOWN_PERIOD/replay") {
                contentType(ContentType.Application.Json)
                setBody("""{"bookId":"BOOK-1"}""")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }
})
