package com.kinetix.risk.service

import com.kinetix.common.model.*
import com.kinetix.risk.client.PositionProvider
import com.kinetix.risk.client.RiskEngineClient
import com.kinetix.risk.kafka.RiskResultPublisher
import com.kinetix.risk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Currency
import java.util.UUID

private val USD = Currency.getInstance("USD")

private fun pos(instrumentId: String = "AAPL") = Position(
    portfolioId = PortfolioId("port-1"),
    instrumentId = InstrumentId(instrumentId),
    assetClass = AssetClass.EQUITY,
    quantity = BigDecimal("100"),
    averageCost = Money(BigDecimal("150.00"), USD),
    marketPrice = Money(BigDecimal("170.00"), USD),
)

private fun result() = ValuationResult(
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

class VaRCalculationServiceManifestCaptureTest : FunSpec({

    val positionProvider = mockk<PositionProvider>()
    val riskEngineClient = mockk<RiskEngineClient>()
    val resultPublisher = mockk<RiskResultPublisher>()
    val jobRecorder = mockk<ValuationJobRecorder>()
    val manifestCapture = mockk<RunManifestCapture>()

    val service = VaRCalculationService(
        positionProvider, riskEngineClient, resultPublisher, SimpleMeterRegistry(),
        jobRecorder = jobRecorder,
        runManifestCapture = manifestCapture,
    )

    beforeEach {
        clearMocks(positionProvider, riskEngineClient, resultPublisher, jobRecorder, manifestCapture)
        coEvery { jobRecorder.save(any()) } just Runs
        coEvery { jobRecorder.update(any()) } just Runs
    }

    test("invokes manifest capture and links manifest ID to completed job") {
        val positions = listOf(pos())
        val expectedResult = result()
        val manifestId = UUID.randomUUID()

        coEvery { positionProvider.getPositions(PortfolioId("port-1")) } returns positions
        coEvery { riskEngineClient.valuate(any(), positions, any()) } returns expectedResult
        coEvery { resultPublisher.publish(expectedResult) } just Runs
        coEvery { manifestCapture.capture(any(), any(), any(), any(), any(), any()) } returns RunManifest(
            manifestId = manifestId,
            jobId = UUID.randomUUID(),
            portfolioId = "port-1",
            valuationDate = LocalDate.now(),
            capturedAt = Instant.now(),
            modelVersion = "",
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

        service.calculateVaR(
            VaRCalculationRequest(
                portfolioId = PortfolioId("port-1"),
                calculationType = CalculationType.PARAMETRIC,
                confidenceLevel = ConfidenceLevel.CL_95,
            )
        )

        coVerify { manifestCapture.capture(any(), any(), positions, any(), any(), any()) }

        val updateSlot = slot<ValuationJob>()
        coVerify { jobRecorder.update(capture(updateSlot)) }
        updateSlot.captured.manifestId shouldBe manifestId
    }

    test("generates MC seed when calculation type is MONTE_CARLO and seed is 0") {
        val positions = listOf(pos())
        val expectedResult = result().copy(calculationType = CalculationType.MONTE_CARLO)
        val manifestId = UUID.randomUUID()

        coEvery { positionProvider.getPositions(PortfolioId("port-1")) } returns positions
        coEvery { riskEngineClient.valuate(any(), positions, any()) } returns expectedResult
        coEvery { resultPublisher.publish(any()) } just Runs
        coEvery { manifestCapture.capture(any(), any(), any(), any(), any(), any()) } returns RunManifest(
            manifestId = manifestId,
            jobId = UUID.randomUUID(),
            portfolioId = "port-1",
            valuationDate = LocalDate.now(),
            capturedAt = Instant.now(),
            modelVersion = "",
            calculationType = "MONTE_CARLO",
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

        service.calculateVaR(
            VaRCalculationRequest(
                portfolioId = PortfolioId("port-1"),
                calculationType = CalculationType.MONTE_CARLO,
                confidenceLevel = ConfidenceLevel.CL_95,
                monteCarloSeed = 0,
            )
        )

        // Verify the request passed to capture has a non-zero seed
        val captureRequestSlot = slot<VaRCalculationRequest>()
        coVerify { manifestCapture.capture(any(), capture(captureRequestSlot), any(), any(), any(), any()) }
        captureRequestSlot.captured.monteCarloSeed shouldBeGreaterThan 0

        // Verify the same seeded request was passed to the risk engine
        val riskEngineRequestSlot = slot<VaRCalculationRequest>()
        coVerify { riskEngineClient.valuate(capture(riskEngineRequestSlot), any(), any()) }
        riskEngineRequestSlot.captured.monteCarloSeed shouldBeGreaterThan 0
    }

    test("does not generate MC seed for non-Monte-Carlo calculations") {
        val positions = listOf(pos())
        val expectedResult = result()
        val manifestId = UUID.randomUUID()

        coEvery { positionProvider.getPositions(PortfolioId("port-1")) } returns positions
        coEvery { riskEngineClient.valuate(any(), positions, any()) } returns expectedResult
        coEvery { resultPublisher.publish(expectedResult) } just Runs
        coEvery { manifestCapture.capture(any(), any(), any(), any(), any(), any()) } returns RunManifest(
            manifestId = manifestId,
            jobId = UUID.randomUUID(),
            portfolioId = "port-1",
            valuationDate = LocalDate.now(),
            capturedAt = Instant.now(),
            modelVersion = "",
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

        service.calculateVaR(
            VaRCalculationRequest(
                portfolioId = PortfolioId("port-1"),
                calculationType = CalculationType.PARAMETRIC,
                confidenceLevel = ConfidenceLevel.CL_95,
                monteCarloSeed = 0,
            )
        )

        val captureRequestSlot = slot<VaRCalculationRequest>()
        coVerify { manifestCapture.capture(any(), capture(captureRequestSlot), any(), any(), any(), any()) }
        captureRequestSlot.captured.monteCarloSeed shouldBe 0
    }

    test("calculation succeeds even if manifest capture fails") {
        val positions = listOf(pos())
        val expectedResult = result()

        coEvery { positionProvider.getPositions(PortfolioId("port-1")) } returns positions
        coEvery { riskEngineClient.valuate(any(), positions, any()) } returns expectedResult
        coEvery { resultPublisher.publish(expectedResult) } just Runs
        coEvery { manifestCapture.capture(any(), any(), any(), any(), any(), any()) } throws RuntimeException("DB down")

        val calcResult = service.calculateVaR(
            VaRCalculationRequest(
                portfolioId = PortfolioId("port-1"),
                calculationType = CalculationType.PARAMETRIC,
                confidenceLevel = ConfidenceLevel.CL_95,
            )
        )

        calcResult.shouldNotBeNull()
        calcResult.varValue shouldBe 5000.0

        // manifestId should be null on the job since capture failed
        val updateSlot = slot<ValuationJob>()
        coVerify { jobRecorder.update(capture(updateSlot)) }
        updateSlot.captured.manifestId shouldBe null
    }
})
