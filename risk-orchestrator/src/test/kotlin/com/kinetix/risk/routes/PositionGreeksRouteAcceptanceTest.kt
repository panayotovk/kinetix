package com.kinetix.risk.routes

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.BookId
import com.kinetix.risk.cache.RedisTestSetup
import com.kinetix.risk.cache.RedisVaRCache
import com.kinetix.risk.model.CalculationType
import com.kinetix.risk.model.ComponentBreakdown
import com.kinetix.risk.model.ConfidenceLevel
import com.kinetix.risk.model.PositionGreek
import com.kinetix.risk.model.ValuationOutput
import com.kinetix.risk.model.ValuationResult
import com.kinetix.risk.routes.dtos.VaRResultResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.time.Instant

private val TEST_INSTANT = Instant.parse("2025-06-01T12:00:00Z")

/**
 * Acceptance tests verifying that per-instrument analytical Black-Scholes Greeks
 * (positionGreeks) flow from the domain model through to the JSON response.
 */
class PositionGreeksRouteAcceptanceTest : FunSpec({

    val varCache = RedisVaRCache(RedisTestSetup.start())

    test("VaR response includes positionGreeks when valuation result has option position Greeks") {
        val result = ValuationResult(
            bookId = BookId("port-opts"),
            calculationType = CalculationType.PARAMETRIC,
            confidenceLevel = ConfidenceLevel.CL_95,
            varValue = 15000.0,
            expectedShortfall = 19500.0,
            componentBreakdown = listOf(ComponentBreakdown(AssetClass.DERIVATIVE, 15000.0, 100.0)),
            greeks = null,
            calculatedAt = TEST_INSTANT,
            computedOutputs = setOf(ValuationOutput.VAR, ValuationOutput.GREEKS),
            positionGreeks = listOf(
                PositionGreek(
                    instrumentId = "AAPL-OPT-JAN25-150C",
                    delta = 0.65,
                    gamma = 0.03,
                    vega = 125.4,
                    theta = -8.2,
                    rho = 12.1,
                ),
                PositionGreek(
                    instrumentId = "MSFT-OPT-FEB25-400P",
                    delta = -0.35,
                    gamma = 0.02,
                    vega = 98.7,
                    theta = -5.6,
                    rho = -7.3,
                ),
            ),
        )
        varCache.put("port-opts", result)

        testApplication {
            install(ContentNegotiation) { json() }
            routing {
                get("/api/v1/risk/var/{bookId}") {
                    val bookId = call.parameters["bookId"]!!
                    val cached = varCache.get(bookId)
                    if (cached != null) {
                        call.respond(cached.toResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            val response = client.get("/api/v1/risk/var/port-opts")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.decodeFromString<VaRResultResponse>(response.bodyAsText())
            val Greeks = body.positionGreeks
            Greeks.shouldNotBeNull()
            Greeks shouldHaveSize 2

            val first = Greeks[0]
            first.instrumentId shouldBe "AAPL-OPT-JAN25-150C"
            first.delta shouldBe "0.650000"
            first.gamma shouldBe "0.030000"
            first.vega shouldBe "125.400000"
            first.theta shouldBe "-8.200000"
            first.rho shouldBe "12.100000"

            val second = Greeks[1]
            second.instrumentId shouldBe "MSFT-OPT-FEB25-400P"
            second.delta shouldBe "-0.350000"
            second.gamma shouldBe "0.020000"
            second.vega shouldBe "98.700000"
            second.theta shouldBe "-5.600000"
            second.rho shouldBe "-7.300000"
        }
    }

    test("VaR response omits positionGreeks field when there are no option position Greeks") {
        val result = ValuationResult(
            bookId = BookId("port-equity"),
            calculationType = CalculationType.PARAMETRIC,
            confidenceLevel = ConfidenceLevel.CL_95,
            varValue = 8000.0,
            expectedShortfall = 10400.0,
            componentBreakdown = listOf(ComponentBreakdown(AssetClass.EQUITY, 8000.0, 100.0)),
            greeks = null,
            calculatedAt = TEST_INSTANT,
            computedOutputs = setOf(ValuationOutput.VAR),
            positionGreeks = emptyList(),
        )
        varCache.put("port-equity", result)

        testApplication {
            install(ContentNegotiation) { json() }
            routing {
                get("/api/v1/risk/var/{bookId}") {
                    val bookId = call.parameters["bookId"]!!
                    val cached = varCache.get(bookId)
                    if (cached != null) {
                        call.respond(cached.toResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            val response = client.get("/api/v1/risk/var/port-equity")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.decodeFromString<VaRResultResponse>(response.bodyAsText())
            body.positionGreeks.shouldBeNull()
        }
    }
})
