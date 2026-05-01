package com.kinetix.risk.routes

import com.kinetix.risk.model.AdaptiveVaRParameters
import com.kinetix.risk.model.CalculationType
import com.kinetix.risk.model.ConfidenceLevel
import com.kinetix.risk.model.EarlyWarning
import com.kinetix.risk.model.MarketRegime
import com.kinetix.risk.model.MarketRegimeHistory
import com.kinetix.risk.model.RegimeSignals
import com.kinetix.risk.model.RegimeState
import com.kinetix.risk.persistence.DatabaseTestSetup
import com.kinetix.risk.persistence.ExposedMarketRegimeRepository
import com.kinetix.risk.persistence.MarketRegimeHistoryTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

private fun crisisState() = RegimeState(
    regime = MarketRegime.CRISIS,
    detectedAt = Instant.parse("2026-03-24T14:30:00Z"),
    confidence = 0.87,
    signals = RegimeSignals(
        realisedVol20d = 0.28,
        crossAssetCorrelation = 0.80,
        creditSpreadBps = 220.0,
        pnlVolatility = 0.07,
    ),
    varParameters = AdaptiveVaRParameters(
        calculationType = CalculationType.MONTE_CARLO,
        confidenceLevel = ConfidenceLevel.CL_99,
        timeHorizonDays = 5,
        correlationMethod = "stressed",
        numSimulations = 50_000,
    ),
    consecutiveObservations = 3,
    isConfirmed = true,
    degradedInputs = false,
)

class MarketRegimeRoutesAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository = ExposedMarketRegimeRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { MarketRegimeHistoryTable.deleteAll() }
    }

    fun testApp(currentState: RegimeState, block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            routing {
                marketRegimeRoutes(
                    currentStateProvider = { currentState },
                    repository = repository,
                )
            }
            block()
        }
    }

    test("GET /api/v1/risk/regime/current returns 200 with regime details") {
        testApp(currentState = crisisState()) {
            val response = client.get("/api/v1/risk/regime/current")

            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /api/v1/risk/regime/current response body contains regime CRISIS") {
        testApp(currentState = crisisState()) {
            val response = client.get("/api/v1/risk/regime/current")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            body["regime"]!!.jsonPrimitive.content shouldBe "CRISIS"
        }
    }

    test("GET /api/v1/risk/regime/current response body contains isConfirmed true") {
        testApp(currentState = crisisState()) {
            val response = client.get("/api/v1/risk/regime/current")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            body["isConfirmed"]!!.jsonPrimitive.content shouldBe "true"
        }
    }

    test("GET /api/v1/risk/regime/current response body contains effective VaR parameters") {
        testApp(currentState = crisisState()) {
            val response = client.get("/api/v1/risk/regime/current")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val params = body["varParameters"]!!.jsonObject

            params["calculationType"]!!.jsonPrimitive.content shouldBe "MONTE_CARLO"
            params["confidenceLevel"]!!.jsonPrimitive.content shouldBe "CL_99"
        }
    }

    test("GET /api/v1/risk/regime/history returns 200 with items array") {
        val state = crisisState()
        repository.insert(state, UUID.randomUUID())
        repository.insert(state, UUID.randomUUID())

        testApp(currentState = state) {
            val response = client.get("/api/v1/risk/regime/history")

            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["items"]!!.jsonArray.size shouldBe 2
        }
    }

    test("GET /api/v1/risk/regime/history respects limit query parameter") {
        val state = crisisState()
        repository.insert(state, UUID.randomUUID())
        repository.insert(state, UUID.randomUUID())
        repository.insert(state, UUID.randomUUID())

        testApp(currentState = state) {
            val response = client.get("/api/v1/risk/regime/history?limit=5")

            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /api/v1/risk/regime/history total matches items size") {
        val state = crisisState()
        repository.insert(state, UUID.randomUUID())
        repository.insert(state, UUID.randomUUID())
        repository.insert(state, UUID.randomUUID())

        testApp(currentState = state) {
            val response = client.get("/api/v1/risk/regime/history")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            body["total"]!!.jsonPrimitive.content.toInt() shouldBe 3
        }
    }

    test("GET /api/v1/risk/regime/current response body contains empty earlyWarnings array when none present") {
        testApp(currentState = crisisState()) {
            val response = client.get("/api/v1/risk/regime/current")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            body["earlyWarnings"]!!.jsonArray.size shouldBe 0
        }
    }

    test("GET /api/v1/risk/regime/current response body contains earlyWarnings with correct fields") {
        val stateWithWarning = crisisState().copy(
            earlyWarnings = listOf(
                EarlyWarning(
                    signalName = "realised_vol",
                    currentValue = 0.13,
                    threshold = 0.15,
                    proximityPct = 86.7,
                    message = "Realised volatility approaching elevated regime threshold",
                )
            )
        )

        testApp(currentState = stateWithWarning) {
            val response = client.get("/api/v1/risk/regime/current")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val warnings = body["earlyWarnings"]!!.jsonArray

            warnings.size shouldBe 1
            val w = warnings[0].jsonObject
            w["signalName"]!!.jsonPrimitive.content shouldBe "realised_vol"
            w["currentValue"]!!.jsonPrimitive.content.toDouble() shouldBe 0.13
            w["threshold"]!!.jsonPrimitive.content.toDouble() shouldBe 0.15
            w["proximityPct"]!!.jsonPrimitive.content.toDouble() shouldBe 86.7
            w["message"]!!.jsonPrimitive.content shouldBe "Realised volatility approaching elevated regime threshold"
        }
    }
})
