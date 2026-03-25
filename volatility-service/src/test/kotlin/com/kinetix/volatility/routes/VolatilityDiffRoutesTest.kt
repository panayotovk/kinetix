package com.kinetix.volatility.routes

import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.VolPoint
import com.kinetix.common.model.VolSurface
import com.kinetix.common.model.VolatilitySource
import com.kinetix.volatility.module
import com.kinetix.volatility.persistence.VolSurfaceRepository
import com.kinetix.volatility.service.VolatilityIngestionService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.Instant

class VolatilityDiffRoutesTest : FunSpec({

    val volSurfaceRepo = mockk<VolSurfaceRepository>()
    val ingestionService = mockk<VolatilityIngestionService>()

    val BASE_DATE = Instant.parse("2026-03-24T12:00:00Z")
    val COMPARE_DATE = Instant.parse("2026-03-23T12:00:00Z")

    val baseSurface = VolSurface(
        instrumentId = InstrumentId("AAPL"),
        asOf = BASE_DATE,
        points = listOf(
            VolPoint(BigDecimal("100"), 30, BigDecimal("0.2500")),
            VolPoint(BigDecimal("110"), 30, BigDecimal("0.2200")),
            VolPoint(BigDecimal("100"), 90, BigDecimal("0.2800")),
        ),
        source = VolatilitySource.BLOOMBERG,
    )

    val compareSurface = VolSurface(
        instrumentId = InstrumentId("AAPL"),
        asOf = COMPARE_DATE,
        points = listOf(
            VolPoint(BigDecimal("100"), 30, BigDecimal("0.2400")),
            VolPoint(BigDecimal("110"), 30, BigDecimal("0.2100")),
            VolPoint(BigDecimal("100"), 90, BigDecimal("0.2700")),
        ),
        source = VolatilitySource.BLOOMBERG,
    )

    test("diff endpoint returns point-by-point vol differences between two surfaces") {
        coEvery { volSurfaceRepo.findLatest(InstrumentId("AAPL")) } returns baseSurface
        coEvery {
            volSurfaceRepo.findAtOrBefore(InstrumentId("AAPL"), COMPARE_DATE)
        } returns compareSurface

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get(
                "/api/v1/volatility/AAPL/surface/diff?compareDate=${COMPARE_DATE}"
            )
            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["instrumentId"]?.jsonPrimitive?.content shouldBe "AAPL"
            body["baseDate"]?.jsonPrimitive?.content shouldBe BASE_DATE.toString()
            body["compareDate"]?.jsonPrimitive?.content shouldBe COMPARE_DATE.toString()

            val diffs = body["diffs"]!!.jsonArray
            diffs.size shouldBe 3

            // 0.2500 - 0.2400 = 0.0100
            val first = diffs[0].jsonObject
            first["strike"]?.jsonPrimitive?.double shouldBe 100.0
            first["maturityDays"]?.jsonPrimitive?.int shouldBe 30
            first["baseVol"]?.jsonPrimitive?.double shouldBe 0.25
            first["compareVol"]?.jsonPrimitive?.double shouldBe 0.24
            // diff is approximately 0.01 (allow floating-point tolerance)
            val diff = first["diff"]?.jsonPrimitive?.double ?: 0.0
            (diff > 0.009 && diff < 0.011) shouldBe true
        }
    }

    test("diff endpoint returns 404 when no base surface exists") {
        coEvery { volSurfaceRepo.findLatest(InstrumentId("UNKNOWN")) } returns null

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get(
                "/api/v1/volatility/UNKNOWN/surface/diff?compareDate=${COMPARE_DATE}"
            )
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("diff endpoint returns 404 when compare surface does not exist") {
        coEvery { volSurfaceRepo.findLatest(InstrumentId("AAPL")) } returns baseSurface
        coEvery {
            volSurfaceRepo.findAtOrBefore(InstrumentId("AAPL"), COMPARE_DATE)
        } returns null

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get(
                "/api/v1/volatility/AAPL/surface/diff?compareDate=${COMPARE_DATE}"
            )
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("diff endpoint returns 400 when compareDate parameter is missing") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get("/api/v1/volatility/AAPL/surface/diff")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("diff endpoint returns 400 when compareDate is not a valid ISO-8601 instant") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get("/api/v1/volatility/AAPL/surface/diff?compareDate=not-a-date")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
