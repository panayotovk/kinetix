package com.kinetix.risk.service

import com.kinetix.common.model.*
import com.kinetix.risk.client.RiskEngineClient
import com.kinetix.risk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Currency
import java.util.UUID

private val USD = Currency.getInstance("USD")

class ReplayServiceTest : FunSpec({

    val manifestRepo = mockk<RunManifestRepository>()
    val blobStore = mockk<MarketDataBlobStore>()
    val riskEngineClient = mockk<RiskEngineClient>()
    val jobRecorder = mockk<ValuationJobRecorder>()

    val replayService = ReplayService(manifestRepo, blobStore, riskEngineClient, jobRecorder)

    val testManifest = RunManifest(
        manifestId = UUID.randomUUID(),
        jobId = UUID.randomUUID(),
        portfolioId = "port-1",
        valuationDate = LocalDate.of(2026, 3, 13),
        capturedAt = Instant.now(),
        modelVersion = "0.1.0-abc12345",
        calculationType = "PARAMETRIC",
        confidenceLevel = "CL_95",
        timeHorizonDays = 1,
        numSimulations = 10_000,
        monteCarloSeed = 0,
        positionCount = 1,
        positionDigest = "abc",
        marketDataDigest = "def",
        inputDigest = "ghi",
        status = ManifestStatus.COMPLETE,
    )

    val testPositionEntries = listOf(
        PositionSnapshotEntry(
            instrumentId = "AAPL",
            assetClass = "EQUITY",
            quantity = BigDecimal("100"),
            averageCostAmount = BigDecimal("150.00"),
            marketPriceAmount = BigDecimal("170.00"),
            currency = "USD",
            marketValueAmount = BigDecimal("17000.00"),
            unrealizedPnlAmount = BigDecimal("2000.00"),
        )
    )

    beforeEach {
        clearMocks(manifestRepo, blobStore, riskEngineClient, jobRecorder)
    }

    test("returns ManifestNotFound when no manifest exists for job") {
        val jobId = UUID.randomUUID()
        coEvery { manifestRepo.findByJobId(jobId) } returns null

        val result = replayService.replay(jobId)
        result.shouldBeInstanceOf<ReplayResult.ManifestNotFound>()
    }

    test("returns Error when position snapshot is empty") {
        val jobId = testManifest.jobId
        coEvery { manifestRepo.findByJobId(jobId) } returns testManifest
        coEvery { manifestRepo.findPositionSnapshot(testManifest.manifestId) } returns emptyList()

        val result = replayService.replay(jobId)
        result.shouldBeInstanceOf<ReplayResult.Error>()
    }

    test("replays a valuation using snapshot positions and no market data") {
        val jobId = testManifest.jobId
        val valuationResult = ValuationResult(
            portfolioId = PortfolioId("port-1"),
            calculationType = CalculationType.PARAMETRIC,
            confidenceLevel = ConfidenceLevel.CL_95,
            varValue = 5000.0,
            expectedShortfall = 6250.0,
            componentBreakdown = listOf(ComponentBreakdown(AssetClass.EQUITY, 5000.0, 100.0)),
            greeks = null,
            calculatedAt = Instant.now(),
            computedOutputs = setOf(ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL),
        )

        coEvery { manifestRepo.findByJobId(jobId) } returns testManifest
        coEvery { manifestRepo.findPositionSnapshot(testManifest.manifestId) } returns testPositionEntries
        coEvery { manifestRepo.findMarketDataRefs(testManifest.manifestId) } returns emptyList()
        coEvery { riskEngineClient.valuate(any(), any(), any()) } returns valuationResult
        coEvery { jobRecorder.findByJobId(jobId) } returns null

        val result = replayService.replay(jobId)
        result.shouldBeInstanceOf<ReplayResult.Success>()

        result.manifest shouldBe testManifest
        result.replayResult.varValue shouldBe 5000.0
        result.replayResult.expectedShortfall shouldBe 6250.0
    }

    test("replays with market data blobs resolved from blob store") {
        val jobId = testManifest.jobId
        val contentHash = "abcdef1234567890"
        val blob = """{"dataType":"SPOT_PRICE","instrumentId":"AAPL","assetClass":"EQUITY","value":170.5}"""

        val refs = listOf(
            MarketDataRef(
                dataType = "SPOT_PRICE",
                instrumentId = "AAPL",
                assetClass = "EQUITY",
                contentHash = contentHash,
                status = MarketDataSnapshotStatus.FETCHED,
                sourceService = "price-service",
                sourcedAt = Instant.now(),
            )
        )

        val valuationResult = ValuationResult(
            portfolioId = PortfolioId("port-1"),
            calculationType = CalculationType.PARAMETRIC,
            confidenceLevel = ConfidenceLevel.CL_95,
            varValue = 5000.0,
            expectedShortfall = 6250.0,
            componentBreakdown = listOf(ComponentBreakdown(AssetClass.EQUITY, 5000.0, 100.0)),
            greeks = null,
            calculatedAt = Instant.now(),
            computedOutputs = setOf(ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL),
        )

        coEvery { manifestRepo.findByJobId(jobId) } returns testManifest
        coEvery { manifestRepo.findPositionSnapshot(testManifest.manifestId) } returns testPositionEntries
        coEvery { manifestRepo.findMarketDataRefs(testManifest.manifestId) } returns refs
        coEvery { blobStore.get(contentHash) } returns blob
        coEvery { riskEngineClient.valuate(any(), any(), any()) } returns valuationResult
        coEvery { jobRecorder.findByJobId(jobId) } returns null

        val result = replayService.replay(jobId)
        result.shouldBeInstanceOf<ReplayResult.Success>()

        // Verify market data was passed to the risk engine
        val marketDataSlot = slot<List<MarketDataValue>>()
        coVerify { riskEngineClient.valuate(any(), any(), capture(marketDataSlot)) }
        marketDataSlot.captured.size shouldBe 1
        val scalar = marketDataSlot.captured[0]
        scalar.shouldBeInstanceOf<ScalarMarketData>()
        scalar.value shouldBe 170.5
    }

    test("skips MISSING market data refs without calling blob store") {
        val jobId = testManifest.jobId
        val refs = listOf(
            MarketDataRef(
                dataType = "YIELD_CURVE",
                instrumentId = "USD_SOFR",
                assetClass = "RATES",
                contentHash = "",
                status = MarketDataSnapshotStatus.MISSING,
                sourceService = "rates-service",
                sourcedAt = Instant.now(),
            )
        )

        val valuationResult = ValuationResult(
            portfolioId = PortfolioId("port-1"),
            calculationType = CalculationType.PARAMETRIC,
            confidenceLevel = ConfidenceLevel.CL_95,
            varValue = 5000.0,
            expectedShortfall = 6250.0,
            componentBreakdown = listOf(ComponentBreakdown(AssetClass.EQUITY, 5000.0, 100.0)),
            greeks = null,
            calculatedAt = Instant.now(),
            computedOutputs = setOf(ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL),
        )

        coEvery { manifestRepo.findByJobId(jobId) } returns testManifest
        coEvery { manifestRepo.findPositionSnapshot(testManifest.manifestId) } returns testPositionEntries
        coEvery { manifestRepo.findMarketDataRefs(testManifest.manifestId) } returns refs
        coEvery { riskEngineClient.valuate(any(), any(), any()) } returns valuationResult
        coEvery { jobRecorder.findByJobId(jobId) } returns null

        val result = replayService.replay(jobId)
        result.shouldBeInstanceOf<ReplayResult.Success>()

        coVerify(exactly = 0) { blobStore.get(any()) }
    }

    test("getManifest returns manifest when it exists") {
        val jobId = testManifest.jobId
        coEvery { manifestRepo.findByJobId(jobId) } returns testManifest

        val manifest = replayService.getManifest(jobId)
        manifest shouldBe testManifest
    }

    test("getManifest returns null when no manifest exists") {
        val jobId = UUID.randomUUID()
        coEvery { manifestRepo.findByJobId(jobId) } returns null

        val manifest = replayService.getManifest(jobId)
        manifest shouldBe null
    }

    test("replay request uses original manifest parameters") {
        val jobId = testManifest.jobId
        val manifest = testManifest.copy(
            calculationType = "MONTE_CARLO",
            numSimulations = 50_000,
            monteCarloSeed = 42,
            timeHorizonDays = 10,
        )

        val valuationResult = ValuationResult(
            portfolioId = PortfolioId("port-1"),
            calculationType = CalculationType.MONTE_CARLO,
            confidenceLevel = ConfidenceLevel.CL_95,
            varValue = 5000.0,
            expectedShortfall = 6250.0,
            componentBreakdown = listOf(ComponentBreakdown(AssetClass.EQUITY, 5000.0, 100.0)),
            greeks = null,
            calculatedAt = Instant.now(),
            computedOutputs = setOf(ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL),
        )

        coEvery { manifestRepo.findByJobId(jobId) } returns manifest
        coEvery { manifestRepo.findPositionSnapshot(manifest.manifestId) } returns testPositionEntries
        coEvery { manifestRepo.findMarketDataRefs(manifest.manifestId) } returns emptyList()
        coEvery { riskEngineClient.valuate(any(), any(), any()) } returns valuationResult
        coEvery { jobRecorder.findByJobId(jobId) } returns null

        replayService.replay(jobId)

        val requestSlot = slot<VaRCalculationRequest>()
        coVerify { riskEngineClient.valuate(capture(requestSlot), any(), any()) }
        requestSlot.captured.calculationType shouldBe CalculationType.MONTE_CARLO
        requestSlot.captured.numSimulations shouldBe 50_000
        requestSlot.captured.monteCarloSeed shouldBe 42
        requestSlot.captured.timeHorizonDays shouldBe 10
    }
})
