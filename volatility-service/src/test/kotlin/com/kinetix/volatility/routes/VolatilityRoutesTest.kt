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
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.mockk.just
import io.mockk.runs
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant

private val NOW = Instant.parse("2026-02-24T10:00:00Z")

class VolatilityRoutesTest : FunSpec({

    val volSurfaceRepo = mockk<VolSurfaceRepository>()
    val ingestionService = mockk<VolatilityIngestionService>()

    beforeEach {
        clearMocks(volSurfaceRepo)
    }

    test("GET surface latest returns 200 with surface") {
        val surface = VolSurface(
            instrumentId = InstrumentId("AAPL"),
            asOf = NOW,
            points = listOf(
                VolPoint(BigDecimal("100"), 30, BigDecimal("0.25")),
                VolPoint(BigDecimal("100"), 90, BigDecimal("0.28")),
            ),
            source = VolatilitySource.BLOOMBERG,
        )
        coEvery { volSurfaceRepo.findLatest(InstrumentId("AAPL")) } returns surface

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get("/api/v1/volatility/AAPL/surface/latest")
            response.status shouldBe HttpStatusCode.OK
            val body = response.bodyAsText()
            body shouldContain "AAPL"
            body shouldContain "0.25"
            body shouldContain "BLOOMBERG"
        }
    }

    test("GET surface latest returns 404 for unknown instrument") {
        coEvery { volSurfaceRepo.findLatest(InstrumentId("UNKNOWN")) } returns null

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get("/api/v1/volatility/UNKNOWN/surface/latest")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("POST surfaces returns 201 Created") {
        coEvery { ingestionService.ingest(any()) } just runs

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.post("/api/v1/volatility/surfaces") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "instrumentId": "AAPL",
                        "points": [{"strike": 100.0, "maturityDays": 30, "impliedVol": 0.25}],
                        "source": "BLOOMBERG"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText() shouldContain "AAPL"
        }
    }

    test("GET surface history returns 200 with surfaces") {
        val surfaces = listOf(
            VolSurface(
                instrumentId = InstrumentId("AAPL"),
                asOf = NOW,
                points = listOf(VolPoint(BigDecimal("100"), 30, BigDecimal("0.25"))),
                source = VolatilitySource.BLOOMBERG,
            ),
        )
        coEvery { volSurfaceRepo.findByTimeRange(InstrumentId("AAPL"), any(), any()) } returns surfaces

        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get("/api/v1/volatility/AAPL/surface/history?from=2026-01-01T00:00:00Z&to=2026-12-31T00:00:00Z")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "AAPL"
        }
    }

    test("POST surfaces returns 400 when points list is empty") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.post("/api/v1/volatility/surfaces") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "instrumentId": "AAPL",
                        "points": [],
                        "source": "BLOOMBERG"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "points must contain at least one entry"
        }
    }

    test("POST surfaces returns 400 when any vol point has zero implied vol") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.post("/api/v1/volatility/surfaces") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "instrumentId": "AAPL",
                        "points": [
                            {"strike": 100.0, "maturityDays": 30, "impliedVol": 0.25},
                            {"strike": 110.0, "maturityDays": 30, "impliedVol": 0.0}
                        ],
                        "source": "BLOOMBERG"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "impliedVol must be positive"
        }
    }

    test("POST surfaces returns 400 when any vol point has negative implied vol") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.post("/api/v1/volatility/surfaces") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "instrumentId": "AAPL",
                        "points": [{"strike": 100.0, "maturityDays": 30, "impliedVol": -0.10}],
                        "source": "BLOOMBERG"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "impliedVol must be positive"
        }
    }

    test("POST surfaces returns 400 when any vol point has non-positive strike") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.post("/api/v1/volatility/surfaces") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "instrumentId": "AAPL",
                        "points": [{"strike": 0.0, "maturityDays": 30, "impliedVol": 0.25}],
                        "source": "BLOOMBERG"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "strike must be positive"
        }
    }

    test("POST surfaces returns 400 when any vol point has non-positive maturityDays") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.post("/api/v1/volatility/surfaces") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "instrumentId": "AAPL",
                        "points": [{"strike": 100.0, "maturityDays": 0, "impliedVol": 0.25}],
                        "source": "BLOOMBERG"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "maturityDays must be positive"
        }
    }

    test("GET surface history returns 400 when missing from parameter") {
        testApplication {
            application { module(volSurfaceRepo, ingestionService) }

            val response = client.get("/api/v1/volatility/AAPL/surface/history?to=2026-12-31T00:00:00Z")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
