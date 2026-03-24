package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.*

private val sampleSeries = buildJsonObject {
    put("bookId", "book-1")
    putJsonArray("snapshots") {
        addJsonObject {
            put("snapshotAt", "2026-03-24T09:30:00Z")
            put("totalPnl", "1500.00")
            put("realisedPnl", "500.00")
            put("unrealisedPnl", "1000.00")
            put("highWaterMark", "1800.00")
            put("trigger", "position_change")
        }
    }
}

class IntradayPnlRoutesTest : FunSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach {
        clearMocks(riskClient)
    }

    test("GET /api/v1/risk/pnl/intraday/{bookId} returns 200 with series") {
        coEvery {
            riskClient.getIntradayPnl("book-1", "2026-03-24T08:00:00Z", "2026-03-24T17:00:00Z")
        } returns sampleSeries

        testApplication {
            install(ContentNegotiation) { json() }
            routing { intradayPnlProxyRoutes(riskClient) }

            val response = client.get(
                "/api/v1/risk/pnl/intraday/book-1" +
                    "?from=2026-03-24T08:00:00Z&to=2026-03-24T17:00:00Z",
            )
            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["bookId"]?.jsonPrimitive?.content shouldBe "book-1"
            body["snapshots"]?.jsonArray?.size shouldBe 1
        }
    }

    test("GET /api/v1/risk/pnl/intraday/{bookId} returns 400 when from is missing") {
        testApplication {
            install(ContentNegotiation) { json() }
            routing { intradayPnlProxyRoutes(riskClient) }

            val response = client.get(
                "/api/v1/risk/pnl/intraday/book-1?to=2026-03-24T17:00:00Z",
            )
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/risk/pnl/intraday/{bookId} returns 400 when to is missing") {
        testApplication {
            install(ContentNegotiation) { json() }
            routing { intradayPnlProxyRoutes(riskClient) }

            val response = client.get(
                "/api/v1/risk/pnl/intraday/book-1?from=2026-03-24T08:00:00Z",
            )
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/risk/pnl/intraday/{bookId} returns 404 when upstream returns null") {
        coEvery {
            riskClient.getIntradayPnl(any(), any(), any())
        } returns null

        testApplication {
            install(ContentNegotiation) { json() }
            routing { intradayPnlProxyRoutes(riskClient) }

            val response = client.get(
                "/api/v1/risk/pnl/intraday/unknown-book" +
                    "?from=2026-03-24T08:00:00Z&to=2026-03-24T17:00:00Z",
            )
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /api/v1/risk/pnl/intraday/{bookId} passes from and to to the risk client") {
        coEvery {
            riskClient.getIntradayPnl("book-1", "2026-03-24T09:00:00Z", "2026-03-24T10:00:00Z")
        } returns sampleSeries

        testApplication {
            install(ContentNegotiation) { json() }
            routing { intradayPnlProxyRoutes(riskClient) }

            client.get(
                "/api/v1/risk/pnl/intraday/book-1" +
                    "?from=2026-03-24T09:00:00Z&to=2026-03-24T10:00:00Z",
            )

            coVerify(exactly = 1) {
                riskClient.getIntradayPnl("book-1", "2026-03-24T09:00:00Z", "2026-03-24T10:00:00Z")
            }
        }
    }
})
