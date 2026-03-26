package com.kinetix.risk.schedule

import com.kinetix.risk.client.PFEPositionInput
import com.kinetix.risk.model.CounterpartyExposureSnapshot
import com.kinetix.risk.model.ExposureAtTenor
import com.kinetix.risk.service.CounterpartyRiskOrchestrationService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalTime

private fun snapshotFor(id: String) = CounterpartyExposureSnapshot(
    counterpartyId = id,
    calculatedAt = Instant.parse("2026-03-19T18:00:00Z"),
    pfeProfile = listOf(ExposureAtTenor(tenor = "1Y", tenorYears = 1.0, expectedExposure = 30_000.0, pfe95 = 50_000.0, pfe99 = 60_000.0)),
    currentNetExposure = 100_000.0,
    peakPfe = 50_000.0,
    cva = 1_200.0,
    cvaEstimated = false,
    nettingSetExposures = emptyList(),
    collateralHeld = 10_000.0,
    collateralPosted = 0.0,
    netNetExposure = 90_000.0,
    wrongWayRiskFlags = emptyList(),
)

class ScheduledCounterpartyRiskCalculatorTest : FunSpec({

    val service = mockk<CounterpartyRiskOrchestrationService>()

    beforeEach {
        clearMocks(service)
    }

    test("runs PFE for each active counterparty after EOD time") {
        coEvery { service.computeAndPersistPFE("CPTY-A", any(), any(), any()) } returns snapshotFor("CPTY-A")
        coEvery { service.computeAndPersistPFE("CPTY-B", any(), any(), any()) } returns snapshotFor("CPTY-B")

        val job = ScheduledCounterpartyRiskCalculator(
            service = service,
            counterpartyIds = { listOf("CPTY-A", "CPTY-B") },
            eodTime = LocalTime.of(18, 0),
            nowProvider = { LocalTime.of(18, 30) },
        )

        job.tick()

        coVerify(exactly = 1) { service.computeAndPersistPFE("CPTY-A", emptyList(), any(), any()) }
        coVerify(exactly = 1) { service.computeAndPersistPFE("CPTY-B", emptyList(), any(), any()) }
    }

    test("skips all counterparties before EOD time") {
        val job = ScheduledCounterpartyRiskCalculator(
            service = service,
            counterpartyIds = { listOf("CPTY-A") },
            eodTime = LocalTime.of(18, 0),
            nowProvider = { LocalTime.of(17, 59) },
        )

        job.tick()

        coVerify(exactly = 0) { service.computeAndPersistPFE(any(), any(), any(), any()) }
    }

    test("continues processing remaining counterparties when one fails") {
        coEvery { service.computeAndPersistPFE("CPTY-A", any(), any(), any()) } throws RuntimeException("gRPC error")
        coEvery { service.computeAndPersistPFE("CPTY-B", any(), any(), any()) } returns snapshotFor("CPTY-B")

        val job = ScheduledCounterpartyRiskCalculator(
            service = service,
            counterpartyIds = { listOf("CPTY-A", "CPTY-B") },
            eodTime = LocalTime.of(18, 0),
            nowProvider = { LocalTime.of(18, 30) },
        )

        job.tick()

        coVerify(exactly = 1) { service.computeAndPersistPFE("CPTY-B", emptyList(), any(), any()) }
    }

    test("runs at exactly EOD time") {
        coEvery { service.computeAndPersistPFE("CPTY-X", any(), any(), any()) } returns snapshotFor("CPTY-X")

        val job = ScheduledCounterpartyRiskCalculator(
            service = service,
            counterpartyIds = { listOf("CPTY-X") },
            eodTime = LocalTime.of(18, 0),
            nowProvider = { LocalTime.of(18, 0) },
        )

        job.tick()

        coVerify(exactly = 1) { service.computeAndPersistPFE("CPTY-X", emptyList(), any(), any()) }
    }

    test("returns the count of successfully computed counterparties") {
        coEvery { service.computeAndPersistPFE("CPTY-A", any(), any(), any()) } returns snapshotFor("CPTY-A")
        coEvery { service.computeAndPersistPFE("CPTY-B", any(), any(), any()) } throws RuntimeException("fail")
        coEvery { service.computeAndPersistPFE("CPTY-C", any(), any(), any()) } returns snapshotFor("CPTY-C")

        val job = ScheduledCounterpartyRiskCalculator(
            service = service,
            counterpartyIds = { listOf("CPTY-A", "CPTY-B", "CPTY-C") },
            eodTime = LocalTime.of(18, 0),
            nowProvider = { LocalTime.of(18, 30) },
        )

        val count = job.tick()

        count shouldBe 2
    }
})
