package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val sampleRecommendation: JsonObject = buildJsonObject {
    put("id", "00000000-0000-0000-0000-000000000001")
    put("bookId", "BOOK-1")
    put("targetMetric", "DELTA")
    put("targetReductionPct", 0.90)
    put("status", "PENDING")
    put("sourceJobId", "job-123")
    put("totalEstimatedCost", 5250.0)
    put("isExpired", false)
}

private val sampleRecommendationList: JsonArray = buildJsonArray {
    add(sampleRecommendation)
}

class HedgeRecommendationRoutesTest : FunSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach {
        clearMocks(riskClient)
        coEvery { riskClient.calculateVaR(any()) } returns null
        coEvery { riskClient.getLatestVaR(any()) } returns null
    }

    test("POST /api/v1/risk/hedge-suggest/{bookId} proxies request and returns 201") {
        coEvery { riskClient.suggestHedge("BOOK-1", any()) } returns sampleRecommendation

        testApplication {
            application { module(riskClient) }
            val response = client.post("/api/v1/risk/hedge-suggest/BOOK-1") {
                contentType(ContentType.Application.Json)
                setBody("""{"targetMetric":"DELTA","targetReductionPct":0.90}""")
            }
            response.status shouldBe HttpStatusCode.Created
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "00000000-0000-0000-0000-000000000001"
            body["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
            body["targetMetric"]?.jsonPrimitive?.content shouldBe "DELTA"
            body["targetReductionPct"]?.jsonPrimitive?.double shouldBe 0.90
            body["status"]?.jsonPrimitive?.content shouldBe "PENDING"
        }
    }

    test("POST /api/v1/risk/hedge-suggest/{bookId} forwards request body to upstream client") {
        val bodySlot = mutableListOf<JsonObject>()
        coEvery { riskClient.suggestHedge("BOOK-1", capture(bodySlot)) } returns sampleRecommendation

        testApplication {
            application { module(riskClient) }
            client.post("/api/v1/risk/hedge-suggest/BOOK-1") {
                contentType(ContentType.Application.Json)
                setBody("""{"targetMetric":"VEGA","targetReductionPct":0.75,"maxSuggestions":3}""")
            }
            coVerify { riskClient.suggestHedge("BOOK-1", any()) }
            bodySlot.first()["targetMetric"]?.jsonPrimitive?.content shouldBe "VEGA"
        }
    }

    test("GET /api/v1/risk/hedge-suggest/{bookId} returns list of recommendations") {
        coEvery { riskClient.getLatestHedgeRecommendations("BOOK-1", 10) } returns sampleRecommendationList

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/hedge-suggest/BOOK-1")
            response.status shouldBe HttpStatusCode.OK
            val arr = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            arr.size shouldBe 1
            arr[0].jsonObject["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
        }
    }

    test("GET /api/v1/risk/hedge-suggest/{bookId} forwards limit query parameter") {
        coEvery { riskClient.getLatestHedgeRecommendations("BOOK-1", 5) } returns sampleRecommendationList

        testApplication {
            application { module(riskClient) }
            client.get("/api/v1/risk/hedge-suggest/BOOK-1?limit=5")
            coVerify { riskClient.getLatestHedgeRecommendations("BOOK-1", 5) }
        }
    }

    test("GET /api/v1/risk/hedge-suggest/{bookId}/{id} returns specific recommendation") {
        coEvery {
            riskClient.getHedgeRecommendation("BOOK-1", "00000000-0000-0000-0000-000000000001")
        } returns sampleRecommendation

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/hedge-suggest/BOOK-1/00000000-0000-0000-0000-000000000001")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "00000000-0000-0000-0000-000000000001"
        }
    }

    test("GET /api/v1/risk/hedge-suggest/{bookId}/{id} returns 404 when not found") {
        coEvery {
            riskClient.getHedgeRecommendation("BOOK-1", "99999999-9999-9999-9999-999999999999")
        } returns null

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/hedge-suggest/BOOK-1/99999999-9999-9999-9999-999999999999")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("POST /api/v1/risk/hedge-suggest/{bookId}/{id}/accept proxies request and returns 200") {
        val accepted = buildJsonObject {
            put("id", "00000000-0000-0000-0000-000000000001")
            put("bookId", "BOOK-1")
            put("status", "ACCEPTED")
            put("acceptedBy", "trader@example.com")
        }
        val bodySlot = mutableListOf<JsonObject>()
        coEvery {
            riskClient.acceptHedgeRecommendation(
                "BOOK-1",
                "00000000-0000-0000-0000-000000000001",
                capture(bodySlot),
            )
        } returns accepted

        testApplication {
            application { module(riskClient) }
            val response = client.post(
                "/api/v1/risk/hedge-suggest/BOOK-1/00000000-0000-0000-0000-000000000001/accept",
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"acceptedBy":"trader@example.com","suggestionIndices":[0,1]}""")
            }
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "ACCEPTED"
            body["acceptedBy"]?.jsonPrimitive?.content shouldBe "trader@example.com"
            bodySlot.first()["acceptedBy"]?.jsonPrimitive?.content shouldBe "trader@example.com"
        }
    }

    test("POST /api/v1/risk/hedge-suggest/{bookId}/{id}/accept returns 404 when not found") {
        coEvery {
            riskClient.acceptHedgeRecommendation(
                "BOOK-1",
                "99999999-9999-9999-9999-999999999999",
                any(),
            )
        } returns null

        testApplication {
            application { module(riskClient) }
            val response = client.post(
                "/api/v1/risk/hedge-suggest/BOOK-1/99999999-9999-9999-9999-999999999999/accept",
            ) {
                contentType(ContentType.Application.Json)
                setBody("""{"acceptedBy":"trader@example.com"}""")
            }
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("POST /api/v1/risk/hedge-suggest/{bookId}/{id}/reject proxies request and returns 200") {
        val rejected = buildJsonObject {
            put("id", "00000000-0000-0000-0000-000000000001")
            put("bookId", "BOOK-1")
            put("status", "REJECTED")
        }
        coEvery {
            riskClient.rejectHedgeRecommendation(
                "BOOK-1",
                "00000000-0000-0000-0000-000000000001",
            )
        } returns rejected

        testApplication {
            application { module(riskClient) }
            val response = client.post(
                "/api/v1/risk/hedge-suggest/BOOK-1/00000000-0000-0000-0000-000000000001/reject",
            )
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "REJECTED"
        }
    }

    test("POST /api/v1/risk/hedge-suggest/{bookId}/{id}/reject returns 404 when not found") {
        coEvery {
            riskClient.rejectHedgeRecommendation(
                "BOOK-1",
                "99999999-9999-9999-9999-999999999999",
            )
        } returns null

        testApplication {
            application { module(riskClient) }
            val response = client.post(
                "/api/v1/risk/hedge-suggest/BOOK-1/99999999-9999-9999-9999-999999999999/reject",
            )
            response.status shouldBe HttpStatusCode.NotFound
        }
    }
})
