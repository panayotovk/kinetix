package com.kinetix.gateway.contract

import com.kinetix.common.security.Role
import com.kinetix.gateway.auth.TestJwtHelper
import com.kinetix.gateway.client.HttpRegulatoryServiceClient
import com.kinetix.gateway.module
import com.kinetix.gateway.testing.BackendStubServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.testing.*
import kotlinx.serialization.json.*

class GatewayStressScenarioContractAcceptanceTest : FunSpec({

    val jwtConfig = TestJwtHelper.testJwtConfig()
    val jwkProvider = TestJwtHelper.testJwkProvider()
    val riskManagerToken = TestJwtHelper.generateToken(userId = "manager@kinetix.com", roles = listOf(Role.RISK_MANAGER))

    // StressScenarioDto shape (what the upstream regulatory-service returns)
    val scenarioDtoJson = """
        {
          "id":"sc-1","name":"Equity Crash","description":"Global equity -30%",
          "shocks":"{\"EQ\":-0.30}","status":"APPROVED","createdBy":"analyst@kinetix.com",
          "approvedBy":"manager@kinetix.com","approvedAt":"2026-01-15T10:00:00Z",
          "createdAt":"2026-01-10T08:00:00Z","scenarioType":"PARAMETRIC"
        }
    """.trimIndent()

    test("gateway routing to regulatory-service stress scenarios — GET /api/v1/stress-scenarios — returns array with expected JSON shape") {
        val backend = BackendStubServer {
            get("/api/v1/stress-scenarios") {
                call.respond(Json.parseToJsonElement("[$scenarioDtoJson]").jsonArray)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val regulatoryClient = HttpRegulatoryServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(jwtConfig, regulatoryClient = regulatoryClient, jwkProvider = jwkProvider) }
                val response = client.get("/api/v1/stress-scenarios") {
                    header(HttpHeaders.Authorization, "Bearer $riskManagerToken")
                }
                response.status shouldBe HttpStatusCode.OK
                val array = Json.parseToJsonElement(response.bodyAsText()).jsonArray
                array.size shouldBe 1
                val item = array[0].jsonObject
                item["id"]?.jsonPrimitive?.content shouldBe "sc-1"
                item["name"]?.jsonPrimitive?.content shouldBe "Equity Crash"
                item["description"]?.jsonPrimitive?.content shouldBe "Global equity -30%"
                item["shocks"]?.jsonPrimitive?.content shouldBe """{"EQ":-0.30}"""
                item["status"]?.jsonPrimitive?.content shouldBe "APPROVED"
                item["createdBy"]?.jsonPrimitive?.content shouldBe "analyst@kinetix.com"
                item["approvedBy"]?.jsonPrimitive?.content shouldBe "manager@kinetix.com"
                item.containsKey("approvedAt") shouldBe true
                item.containsKey("createdAt") shouldBe true

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/stress-scenarios" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to regulatory-service stress scenarios — POST /api/v1/stress-scenarios with valid body — returns 201 with scenario response shape") {
        val newScenarioDtoJson = """
            {
              "id":"sc-new","name":"FX Shock","description":"USD/EUR +15%",
              "shocks":"{\"FX\":0.15}","status":"DRAFT","createdBy":"analyst@kinetix.com",
              "approvedBy":null,"approvedAt":null,"createdAt":"2026-01-15T10:00:00Z",
              "scenarioType":"PARAMETRIC"
            }
        """.trimIndent()
        val backend = BackendStubServer {
            post("/api/v1/stress-scenarios") {
                call.respond(HttpStatusCode.Created, Json.parseToJsonElement(newScenarioDtoJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val regulatoryClient = HttpRegulatoryServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(jwtConfig, regulatoryClient = regulatoryClient, jwkProvider = jwkProvider) }
                val response = client.post("/api/v1/stress-scenarios") {
                    header(HttpHeaders.Authorization, "Bearer $riskManagerToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "name": "FX Shock",
                            "description": "USD/EUR +15%",
                            "shocks": "{\"FX\":0.15}"
                        }
                        """.trimIndent()
                    )
                }
                response.status shouldBe HttpStatusCode.Created
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["id"]?.jsonPrimitive?.content shouldBe "sc-new"
                body["status"]?.jsonPrimitive?.content shouldBe "DRAFT"
                body["name"]?.jsonPrimitive?.content shouldBe "FX Shock"
                body["approvedBy"] shouldBe JsonNull
                body["approvedAt"] shouldBe JsonNull

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/stress-scenarios" && it.method == "POST" }
                recorded.method shouldBe "POST"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to regulatory-service stress scenarios — PATCH /{id}/approve with approvedBy — returns 200 with APPROVED status and approver") {
        val approvedDtoJson = """
            {
              "id":"sc-1","name":"Equity Crash","description":"Global equity -30%",
              "shocks":"{\"EQ\":-0.30}","status":"APPROVED","createdBy":"analyst@kinetix.com",
              "approvedBy":"manager@kinetix.com","approvedAt":"2026-01-16T12:00:00Z",
              "createdAt":"2026-01-10T08:00:00Z","scenarioType":"PARAMETRIC"
            }
        """.trimIndent()
        val backend = BackendStubServer {
            patch("/api/v1/stress-scenarios/sc-1/approve") {
                call.respond(Json.parseToJsonElement(approvedDtoJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val regulatoryClient = HttpRegulatoryServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(jwtConfig, regulatoryClient = regulatoryClient, jwkProvider = jwkProvider) }
                val response = client.patch("/api/v1/stress-scenarios/sc-1/approve") {
                    header(HttpHeaders.Authorization, "Bearer $riskManagerToken")
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["status"]?.jsonPrimitive?.content shouldBe "APPROVED"
                body["approvedBy"]?.jsonPrimitive?.content shouldBe "manager@kinetix.com"
                body.containsKey("approvedAt") shouldBe true

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/stress-scenarios/sc-1/approve" }
                recorded.method shouldBe "PATCH"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to regulatory-service stress scenarios — PATCH /{id}/retire — returns 200 with RETIRED status") {
        val retiredDtoJson = """
            {
              "id":"sc-1","name":"Equity Crash","description":"Global equity -30%",
              "shocks":"{\"EQ\":-0.30}","status":"RETIRED","createdBy":"analyst@kinetix.com",
              "approvedBy":"manager@kinetix.com","approvedAt":"2026-01-16T12:00:00Z",
              "createdAt":"2026-01-10T08:00:00Z","scenarioType":"PARAMETRIC"
            }
        """.trimIndent()
        val backend = BackendStubServer {
            patch("/api/v1/stress-scenarios/sc-1/retire") {
                call.respond(Json.parseToJsonElement(retiredDtoJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val regulatoryClient = HttpRegulatoryServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(jwtConfig, regulatoryClient = regulatoryClient, jwkProvider = jwkProvider) }
                val response = client.patch("/api/v1/stress-scenarios/sc-1/retire") {
                    header(HttpHeaders.Authorization, "Bearer $riskManagerToken")
                }
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["status"]?.jsonPrimitive?.content shouldBe "RETIRED"
                body["id"]?.jsonPrimitive?.content shouldBe "sc-1"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/stress-scenarios/sc-1/retire" }
                recorded.method shouldBe "PATCH"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
