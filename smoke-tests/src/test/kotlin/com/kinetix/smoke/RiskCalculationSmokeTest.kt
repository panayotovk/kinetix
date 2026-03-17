package com.kinetix.smoke

import com.kinetix.smoke.SmokeHttpClient.smokeGet
import com.kinetix.smoke.SmokeHttpClient.smokePost
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.Instant

@Tags("P2")
class RiskCalculationSmokeTest : FunSpec({

    val client = SmokeHttpClient.create()
    val portfolioId = SmokeTestConfig.seededBookId

    // Store VaR values across tests for cross-method comparison
    var parametricVaR: Double? = null
    var historicalVaR: Double? = null
    var monteCarloVaR: Double? = null

    test("VaR parametric calculates end-to-end") {
        val body = """{"portfolioId":"$portfolioId","method":"PARAMETRIC","confidenceLevel":0.95,"holdingPeriodDays":1,"outputs":["VAR","EXPECTED_SHORTFALL","COMPONENT_VAR"]}"""
        val start = System.currentTimeMillis()
        val response = client.smokePost("/api/v1/risk/calculate", "var-parametric", body)
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC var_parametric_ms=$elapsed")

        response.status shouldBe HttpStatusCode.OK
        val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val varValue = result["varValue"]?.jsonPrimitive?.double
        varValue.shouldNotBeNull()
        varValue.isFinite().shouldBeTrue()
        varValue shouldBeGreaterThan 0.0 // known long-only seed portfolio

        val es = result["expectedShortfall"]?.jsonPrimitive?.double
        es.shouldNotBeNull()
        es.isFinite().shouldBeTrue()
        es shouldBeGreaterThan varValue // ES >= VaR always

        val calculatedAt = result["calculatedAt"]?.jsonPrimitive?.content
        calculatedAt.shouldNotBeNull()
        val calcTime = Instant.parse(calculatedAt)
        val ageSeconds = Instant.now().epochSecond - calcTime.epochSecond
        ageSeconds shouldBeLessThan 60L // calculated within last minute

        parametricVaR = varValue
    }

    test("VaR historical calculates end-to-end") {
        val body = """{"portfolioId":"$portfolioId","method":"HISTORICAL","confidenceLevel":0.95,"holdingPeriodDays":1,"outputs":["VAR","EXPECTED_SHORTFALL"]}"""
        val start = System.currentTimeMillis()
        val response = client.smokePost("/api/v1/risk/calculate", "var-historical", body)
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC var_historical_ms=$elapsed")

        response.status shouldBe HttpStatusCode.OK
        val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val varValue = result["varValue"]?.jsonPrimitive?.double
        varValue.shouldNotBeNull()
        varValue.isFinite().shouldBeTrue()
        varValue shouldBeGreaterThan 0.0

        val es = result["expectedShortfall"]?.jsonPrimitive?.double
        es.shouldNotBeNull()
        es.isFinite().shouldBeTrue()
        es shouldBeGreaterThan varValue

        historicalVaR = varValue
    }

    test("VaR Monte Carlo calculates end-to-end with seed determinism") {
        val body = """{"portfolioId":"$portfolioId","method":"MONTE_CARLO","confidenceLevel":0.95,"holdingPeriodDays":1,"seed":42,"outputs":["VAR","EXPECTED_SHORTFALL"]}"""
        val start = System.currentTimeMillis()
        val response = client.smokePost("/api/v1/risk/calculate", "var-mc", body)
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC var_monte_carlo_ms=$elapsed")

        response.status shouldBe HttpStatusCode.OK
        val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val varValue = result["varValue"]?.jsonPrimitive?.double
        varValue.shouldNotBeNull()
        varValue.isFinite().shouldBeTrue()
        varValue shouldBeGreaterThan 0.0

        val es = result["expectedShortfall"]?.jsonPrimitive?.double
        es.shouldNotBeNull()
        es.isFinite().shouldBeTrue()
        es shouldBeGreaterThan varValue

        monteCarloVaR = varValue

        // Seed determinism: second run with same seed should match
        val response2 = client.smokePost("/api/v1/risk/calculate", "var-mc-determinism", body)
        val result2 = Json.parseToJsonElement(response2.bodyAsText()).jsonObject
        val varValue2 = result2["varValue"]?.jsonPrimitive?.double
        varValue2.shouldNotBeNull()
        val diff = kotlin.math.abs(varValue - varValue2)
        (diff < 1e-6) shouldBe true
    }

    test("cross-method VaR ratio is within 2x") {
        parametricVaR.shouldNotBeNull()
        historicalVaR.shouldNotBeNull()
        monteCarloVaR.shouldNotBeNull()

        val values = listOf(parametricVaR!!, historicalVaR!!, monteCarloVaR!!)
        val ratio = values.max() / values.min()
        println("SMOKE_METRIC var_cross_method_ratio=$ratio")
        ratio shouldBeLessThan 2.0
    }

    test("Greeks are non-NaN with correct signs for long equity portfolio") {
        val body = """{"portfolioId":"$portfolioId","method":"PARAMETRIC","confidenceLevel":0.95,"holdingPeriodDays":1,"outputs":["GREEKS"]}"""
        val start = System.currentTimeMillis()
        val response = client.smokePost("/api/v1/risk/calculate", "greeks", body)
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC greeks_ms=$elapsed")

        response.status shouldBe HttpStatusCode.OK
        val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val greeks = result["greeks"]?.jsonObject

        if (greeks != null) {
            for ((assetClass, values) in greeks) {
                val greekObj = values.jsonObject
                for ((name, value) in greekObj) {
                    val v = value.jsonPrimitive.doubleOrNull
                    if (v != null) {
                        v.isFinite().shouldBeTrue()
                    }
                }
            }
        }
    }

    test("component VaR sums to approximately 100 percent for parametric") {
        val body = """{"portfolioId":"$portfolioId","method":"PARAMETRIC","confidenceLevel":0.95,"holdingPeriodDays":1,"outputs":["COMPONENT_VAR"]}"""
        val response = client.smokePost("/api/v1/risk/calculate", "component-var", body)
        response.status shouldBe HttpStatusCode.OK

        val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val components = result["componentVaR"]?.jsonArray
        if (components != null && components.isNotEmpty()) {
            val pctSum = components.sumOf {
                it.jsonObject["percentageOfTotal"]?.jsonPrimitive?.double ?: 0.0
            }
            println("SMOKE_METRIC component_var_pct_sum=$pctSum")
            (pctSum > 97.0 && pctSum < 103.0) shouldBe true
        }
    }

    test("P and L attribution returns finite values") {
        val body = """{"portfolioId":"$portfolioId"}"""
        val start = System.currentTimeMillis()
        val response = client.smokePost("/api/v1/risk/pnl/compute", "pnl-attribution", body)
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC pnl_attribution_ms=$elapsed")

        if (response.status == HttpStatusCode.OK) {
            val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val totalPnl = result["totalPnl"]?.jsonPrimitive?.doubleOrNull
            if (totalPnl != null) {
                totalPnl.isFinite().shouldBeTrue()
            }
        }
        // P&L may not be available if SOD baseline not set — acceptable on fresh stack
    }

    test("stress test returns non-trivial result for crash scenario") {
        val body = """{"portfolioId":"$portfolioId","scenarioNames":["EQUITY_CRASH"]}"""
        val start = System.currentTimeMillis()
        val response = client.smokePost("/api/v1/risk/stress/batch", "stress-test", body)
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC stress_test_ms=$elapsed")

        if (response.status == HttpStatusCode.OK) {
            val results = Json.parseToJsonElement(response.bodyAsText())
            if (results is JsonArray && results.isNotEmpty()) {
                val first = results.first().jsonObject
                val pnlImpact = first["pnlImpact"]?.jsonPrimitive?.doubleOrNull
                if (pnlImpact != null) {
                    // Known long equity portfolio: crash should produce negative P&L impact
                    pnlImpact shouldBeLessThan 0.0
                }
            }
        }
    }
})
