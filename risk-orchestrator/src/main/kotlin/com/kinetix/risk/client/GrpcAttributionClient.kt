package com.kinetix.risk.client

import com.kinetix.proto.risk.AttributionServiceGrpcKt.AttributionServiceCoroutineStub
import com.kinetix.proto.risk.AttributionPeriod
import com.kinetix.proto.risk.BrinsonAttributionRequest
import com.kinetix.proto.risk.SectorInput as ProtoSectorInput
import com.kinetix.risk.model.BrinsonAttributionResult
import com.kinetix.risk.model.BrinsonSectorAttribution
import java.util.concurrent.TimeUnit

class GrpcAttributionClient(
    private val stub: AttributionServiceCoroutineStub,
    private val deadlineMs: Long = 30_000,
) : AttributionEngineClient {

    override suspend fun calculateBrinsonAttribution(sectors: List<SectorInput>): BrinsonAttributionResult {
        val protoSectors = sectors.map { sector ->
            ProtoSectorInput.newBuilder()
                .setSectorLabel(sector.sectorLabel)
                .setPortfolioWeight(sector.portfolioWeight)
                .setBenchmarkWeight(sector.benchmarkWeight)
                .setPortfolioReturn(sector.portfolioReturn)
                .setBenchmarkReturn(sector.benchmarkReturn)
                .build()
        }

        val period = AttributionPeriod.newBuilder()
            .addAllSectors(protoSectors)
            .setTotalBenchmarkReturn(0.0)
            .build()

        val request = BrinsonAttributionRequest.newBuilder()
            .addPeriods(period)
            .build()

        val response = stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .calculateBrinsonAttribution(request)

        return BrinsonAttributionResult(
            sectors = response.sectorsList.map { s ->
                BrinsonSectorAttribution(
                    sectorLabel = s.sectorLabel,
                    portfolioWeight = s.portfolioWeight,
                    benchmarkWeight = s.benchmarkWeight,
                    portfolioReturn = s.portfolioReturn,
                    benchmarkReturn = s.benchmarkReturn,
                    allocationEffect = s.allocationEffect,
                    selectionEffect = s.selectionEffect,
                    interactionEffect = s.interactionEffect,
                    totalActiveContribution = s.totalActiveContribution,
                )
            },
            totalActiveReturn = response.totalActiveReturn,
            totalAllocationEffect = response.totalAllocationEffect,
            totalSelectionEffect = response.totalSelectionEffect,
            totalInteractionEffect = response.totalInteractionEffect,
        )
    }
}
