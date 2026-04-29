package com.kinetix.gateway.contract

import com.kinetix.gateway.client.MarginEstimateSummary
import com.kinetix.gateway.client.RiskServiceClient
import com.kinetix.gateway.client.UpstreamErrorException
import com.kinetix.gateway.module
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.*

class GatewayMarginContractAcceptanceTest : FunSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach { clearMocks(riskClient) }

    val sampleEstimate = MarginEstimateSummary(
        initialMargin = "12500.00",
        variationMargin = "350.00",
        totalMargin = "12850.00",
        currency = "USD",
    )

    test("GET /api/v1/books/{bookId}/margin proxies the upstream margin estimate as JSON") {
        coEvery { riskClient.getMarginEstimate("BOOK-001", null) } returns sampleEstimate

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/books/BOOK-001/margin")

            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["initialMargin"]?.jsonPrimitive?.content shouldBe "12500.00"
            body["variationMargin"]?.jsonPrimitive?.content shouldBe "350.00"
            body["totalMargin"]?.jsonPrimitive?.content shouldBe "12850.00"
            body["currency"]?.jsonPrimitive?.content shouldBe "USD"
        }
    }

    test("GET /api/v1/books/{bookId}/margin forwards the previousMTM query parameter to the upstream client") {
        coEvery { riskClient.getMarginEstimate("BOOK-001", "9876.50") } returns sampleEstimate

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/books/BOOK-001/margin?previousMTM=9876.50")

            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { riskClient.getMarginEstimate("BOOK-001", "9876.50") }
        }
    }

    test("GET /api/v1/books/{bookId}/margin returns 404 when the upstream reports the book is unknown") {
        coEvery { riskClient.getMarginEstimate("UNKNOWN", null) } returns null

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/books/UNKNOWN/margin")

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /api/v1/books/{bookId}/margin surfaces an upstream 503 as 503") {
        coEvery { riskClient.getMarginEstimate("BOOK-001", null) } throws UpstreamErrorException(503, "service unavailable")

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/books/BOOK-001/margin")

            response.status shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
})
