package com.kinetix.smoke

import com.kinetix.smoke.SmokeHttpClient.smokeGet
import com.kinetix.smoke.SmokeHttpClient.smokePost
import com.kinetix.smoke.SmokeHttpClient.smokePut
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.math.BigDecimal

@Tags("P1")
class TradingWorkflowSmokeTest : FunSpec({

    val client = SmokeHttpClient.create()
    val smokePortfolioId = "smoke-${System.currentTimeMillis()}"
    var bookedTradeId: String? = null

    test("trade booking round-trip — POST trade, verify position appears") {
        val tradeBody = """
        {
            "portfolioId": "$smokePortfolioId",
            "instrumentId": "${SmokeTestConfig.seededInstrumentId}",
            "side": "BUY",
            "quantity": 10,
            "price": { "amount": "150.00", "currency": "USD" },
            "assetClass": "EQUITY"
        }
        """.trimIndent()

        val start = System.currentTimeMillis()
        val response = client.smokePost(
            "/api/v1/portfolios/$smokePortfolioId/trades",
            "trade-booking",
            tradeBody,
        )
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC trade_booking_ms=$elapsed")

        response.status shouldBe HttpStatusCode.Created
        val trade = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        bookedTradeId = trade["tradeId"]?.jsonPrimitive?.content
        bookedTradeId.shouldNotBeNull()

        // Verify position appears
        val posResponse = client.smokeGet(
            "/api/v1/portfolios/$smokePortfolioId/positions",
            "position-check",
        )
        posResponse.status shouldBe HttpStatusCode.OK
        val positions = Json.parseToJsonElement(posResponse.bodyAsText()).jsonArray
        positions.size shouldBeGreaterThan 0

        val position = positions.first().jsonObject
        val qty = position["quantity"]?.jsonPrimitive?.content?.let { BigDecimal(it) }
        qty.shouldNotBeNull()
        qty shouldBeGreaterThan BigDecimal.ZERO
    }

    test("audit event created — trade booking produces audit trail") {
        bookedTradeId.shouldNotBeNull()

        val auditEvent = PollUtils.pollUntil(
            timeoutMs = 30_000,
            intervalMs = 1_000,
            description = "audit event for trade $bookedTradeId",
        ) {
            val response = client.smokeGet(
                "/api/v1/audit/events?portfolioId=$smokePortfolioId",
                "audit-check",
            )
            if (response.status != HttpStatusCode.OK) return@pollUntil null
            val events = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            if (events.isEmpty()) return@pollUntil null
            events.firstOrNull { event ->
                event.jsonObject["tradeId"]?.jsonPrimitive?.content == bookedTradeId
            }
        }

        val eventObj = auditEvent.jsonObject
        eventObj["tradeId"]?.jsonPrimitive?.content shouldBe bookedTradeId
    }

    test("audit hash chain intact — verify endpoint returns valid") {
        val start = System.currentTimeMillis()
        val response = client.smokeGet(
            "/api/v1/audit/verify?portfolioId=$smokePortfolioId",
            "audit-verify",
        )
        val elapsed = System.currentTimeMillis() - start
        println("SMOKE_METRIC audit_verify_ms=$elapsed")

        response.status shouldBe HttpStatusCode.OK
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["valid"]?.jsonPrimitive?.boolean shouldBe true
    }

    test("trade amendment wiring — PUT amendment updates position") {
        bookedTradeId.shouldNotBeNull()

        val amendBody = """
        {
            "quantity": 15,
            "price": { "amount": "155.00", "currency": "USD" },
            "reason": "smoke test amendment"
        }
        """.trimIndent()

        val response = client.smokePut(
            "/api/v1/portfolios/$smokePortfolioId/trades/$bookedTradeId",
            "trade-amend",
            amendBody,
        )
        response.status shouldBe HttpStatusCode.OK

        // Verify updated position
        val posResponse = client.smokeGet(
            "/api/v1/portfolios/$smokePortfolioId/positions",
            "position-after-amend",
        )
        posResponse.status shouldBe HttpStatusCode.OK
        val positions = Json.parseToJsonElement(posResponse.bodyAsText()).jsonArray
        positions.size shouldBeGreaterThan 0
    }
})
