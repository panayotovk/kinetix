package com.kinetix.risk.routes

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.BookId
import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.Money
import com.kinetix.common.model.Position
import com.kinetix.risk.client.AttributionEngineClient
import com.kinetix.risk.client.BenchmarkServiceClient
import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.PositionProvider
import com.kinetix.risk.client.SectorInput
import com.kinetix.risk.client.dtos.BenchmarkConstituentDto
import com.kinetix.risk.client.dtos.BenchmarkDetailDto
import com.kinetix.risk.model.BrinsonAttributionResult
import com.kinetix.risk.model.BrinsonSectorAttribution
import com.kinetix.risk.routes.dtos.BrinsonAttributionResponse
import com.kinetix.risk.service.BenchmarkAttributionService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

private val USD = Currency.getInstance("USD")
private val BOOK_ID = BookId("BOOK-EQ-01")
private val TODAY = LocalDate.of(2026, 3, 25)

private fun position(instrumentId: String, marketValue: Double) = Position(
    bookId = BOOK_ID,
    instrumentId = InstrumentId(instrumentId),
    quantity = BigDecimal.ONE,
    averageCost = Money(BigDecimal.valueOf(marketValue), USD),
    marketPrice = Money(BigDecimal.valueOf(marketValue), USD),
    assetClass = AssetClass.EQUITY,
)

private val POSITIONS = listOf(
    position("AAPL", 700_000.0),
    position("MSFT", 300_000.0),
)

private val BENCHMARK_DETAIL = BenchmarkDetailDto(
    benchmarkId = "SP500",
    name = "S&P 500",
    description = null,
    createdAt = "2026-01-01T00:00:00Z",
    constituents = listOf(
        BenchmarkConstituentDto("AAPL", "0.0700", "2026-03-25"),
        BenchmarkConstituentDto("MSFT", "0.0650", "2026-03-25"),
    ),
)

private val ATTRIBUTION_RESULT = BrinsonAttributionResult(
    sectors = listOf(
        BrinsonSectorAttribution(
            sectorLabel = "AAPL",
            portfolioWeight = 0.70,
            benchmarkWeight = 0.07,
            portfolioReturn = 0.0,
            benchmarkReturn = 0.0,
            allocationEffect = 0.028,
            selectionEffect = 0.0,
            interactionEffect = 0.0,
            totalActiveContribution = 0.028,
        ),
        BrinsonSectorAttribution(
            sectorLabel = "MSFT",
            portfolioWeight = 0.30,
            benchmarkWeight = 0.065,
            portfolioReturn = 0.0,
            benchmarkReturn = 0.0,
            allocationEffect = 0.012,
            selectionEffect = 0.0,
            interactionEffect = 0.0,
            totalActiveContribution = 0.012,
        ),
    ),
    totalActiveReturn = 0.040,
    totalAllocationEffect = 0.040,
    totalSelectionEffect = 0.0,
    totalInteractionEffect = 0.0,
)

private fun Application.configureTestApp(service: BenchmarkAttributionService) {
    install(ContentNegotiation) { json() }
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(
                cause.message ?: "Bad request",
                status = HttpStatusCode.BadRequest,
            )
        }
    }
    routing {
        benchmarkAttributionRoutes(service)
    }
}

class BenchmarkAttributionRoutesAcceptanceTest : FunSpec({

    val positionProvider = mockk<PositionProvider>()
    val benchmarkServiceClient = mockk<BenchmarkServiceClient>()
    val attributionEngineClient = mockk<AttributionEngineClient>()
    val service = BenchmarkAttributionService(
        positionProvider = positionProvider,
        benchmarkServiceClient = benchmarkServiceClient,
        attributionEngineClient = attributionEngineClient,
    )

    beforeEach { clearMocks(positionProvider, benchmarkServiceClient, attributionEngineClient) }

    test("GET /api/v1/books/{bookId}/attribution returns Brinson attribution result") {
        coEvery { positionProvider.getPositions(BOOK_ID) } returns POSITIONS
        coEvery { benchmarkServiceClient.getBenchmarkDetail("SP500", TODAY) } returns ClientResponse.Success(BENCHMARK_DETAIL)
        coEvery { attributionEngineClient.calculateBrinsonAttribution(any()) } returns ATTRIBUTION_RESULT

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution?benchmarkId=SP500&asOfDate=2026-03-25")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.decodeFromString<BrinsonAttributionResponse>(response.bodyAsText())
            body.bookId shouldBe "BOOK-EQ-01"
            body.benchmarkId shouldBe "SP500"
            body.asOfDate shouldBe "2026-03-25"
            body.sectors.size shouldBe 2
            body.totalActiveReturn shouldBe 0.040
            body.totalAllocationEffect shouldBe 0.040
        }
    }

    test("GET /api/v1/books/{bookId}/attribution returns sectors with allocation and selection effects") {
        coEvery { positionProvider.getPositions(BOOK_ID) } returns POSITIONS
        coEvery { benchmarkServiceClient.getBenchmarkDetail("SP500", TODAY) } returns ClientResponse.Success(BENCHMARK_DETAIL)
        coEvery { attributionEngineClient.calculateBrinsonAttribution(any()) } returns ATTRIBUTION_RESULT

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution?benchmarkId=SP500&asOfDate=2026-03-25")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.decodeFromString<BrinsonAttributionResponse>(response.bodyAsText())
            val aaplSector = body.sectors.find { it.sectorLabel == "AAPL" }
            aaplSector?.portfolioWeight shouldBe 0.70
            aaplSector?.benchmarkWeight shouldBe 0.07
            aaplSector?.allocationEffect shouldBe 0.028
        }
    }

    test("GET /api/v1/books/{bookId}/attribution uses today when asOfDate is omitted") {
        coEvery { positionProvider.getPositions(BOOK_ID) } returns POSITIONS
        coEvery { benchmarkServiceClient.getBenchmarkDetail("SP500", any()) } returns ClientResponse.Success(BENCHMARK_DETAIL)
        coEvery { attributionEngineClient.calculateBrinsonAttribution(any()) } returns ATTRIBUTION_RESULT

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution?benchmarkId=SP500")
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /api/v1/books/{bookId}/attribution returns 400 when benchmarkId is missing") {
        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/books/{bookId}/attribution returns 400 when asOfDate is invalid") {
        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution?benchmarkId=SP500&asOfDate=not-a-date")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/books/{bookId}/attribution returns 400 when benchmark not found") {
        coEvery { positionProvider.getPositions(BOOK_ID) } returns POSITIONS
        coEvery { benchmarkServiceClient.getBenchmarkDetail("MISSING", TODAY) } returns ClientResponse.NotFound(404)

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution?benchmarkId=MISSING&asOfDate=2026-03-25")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/books/{bookId}/attribution returns 400 when book has no positions") {
        coEvery { positionProvider.getPositions(BOOK_ID) } returns emptyList()

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/books/BOOK-EQ-01/attribution?benchmarkId=SP500&asOfDate=2026-03-25")
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
