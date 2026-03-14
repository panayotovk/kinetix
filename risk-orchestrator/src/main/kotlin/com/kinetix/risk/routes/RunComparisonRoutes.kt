package com.kinetix.risk.routes

import com.kinetix.common.model.PortfolioId
import com.kinetix.risk.client.PositionProvider
import com.kinetix.risk.client.RiskEngineClient
import com.kinetix.risk.mapper.toResponse
import com.kinetix.risk.mapper.toRunSnapshot
import com.kinetix.risk.model.CalculationType
import com.kinetix.risk.model.ComparisonType
import com.kinetix.risk.model.ConfidenceLevel
import com.kinetix.risk.model.VaRCalculationRequest
import com.kinetix.risk.routes.dtos.MarketDataQuantDiffResponse
import com.kinetix.risk.routes.dtos.ModelComparisonRequestBody
import com.kinetix.risk.routes.dtos.RunComparisonRequestBody
import com.kinetix.risk.service.MarketDataBlobStore
import com.kinetix.risk.service.MarketDataQuantDiffer
import com.kinetix.risk.service.RunComparisonService
import com.kinetix.risk.service.RunManifestRepository
import com.kinetix.risk.service.VaRAttributionService
import com.kinetix.risk.service.ValuationJobRecorder
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.util.UUID

fun Route.runComparisonRoutes(
    runComparisonService: RunComparisonService,
    jobRecorder: ValuationJobRecorder,
    varAttributionService: VaRAttributionService? = null,
    riskEngineClient: RiskEngineClient? = null,
    positionProvider: PositionProvider? = null,
    manifestRepo: RunManifestRepository? = null,
    blobStore: MarketDataBlobStore? = null,
    marketDataQuantDiffer: MarketDataQuantDiffer? = null,
) {
    // Compare two runs by job IDs
    post("/api/v1/risk/compare/{portfolioId}") {
        val portfolioId = call.requirePathParam("portfolioId")
        val body = call.receive<RunComparisonRequestBody>()

        val baseJobId = body.baseJobId
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "baseJobId is required"))
        val targetJobId = body.targetJobId
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "targetJobId is required"))

        val comparison = runComparisonService.compareByJobIds(
            UUID.fromString(baseJobId),
            UUID.fromString(targetJobId),
        )
        call.respond(comparison.toResponse())
    }

    // Day-over-day comparison
    get("/api/v1/risk/compare/{portfolioId}/day-over-day") {
        val portfolioId = call.requirePathParam("portfolioId")
        val targetDateStr = call.request.queryParameters["targetDate"]
        val baseDateStr = call.request.queryParameters["baseDate"]

        val targetDate = if (targetDateStr != null) {
            try {
                LocalDate.parse(targetDateStr)
            } catch (_: Exception) {
                return@get call.respond(HttpStatusCode.BadRequest, "Invalid targetDate format. Expected YYYY-MM-DD.")
            }
        } else {
            LocalDate.now()
        }
        val baseDate = if (baseDateStr != null) {
            try {
                LocalDate.parse(baseDateStr)
            } catch (_: Exception) {
                return@get call.respond(HttpStatusCode.BadRequest, "Invalid baseDate format. Expected YYYY-MM-DD.")
            }
        } else {
            targetDate.minusDays(1)
        }

        val comparison = runComparisonService.compareDayOverDay(portfolioId, targetDate, baseDate)
        call.respond(comparison.toResponse())
    }

    // VaR attribution (expensive — only invoked on explicit user request)
    if (varAttributionService != null) {
        post("/api/v1/risk/compare/{portfolioId}/day-over-day/attribution") {
            val portfolioId = call.requirePathParam("portfolioId")
            val targetDateStr = call.request.queryParameters["targetDate"]
            val baseDateStr = call.request.queryParameters["baseDate"]

            val targetDate = targetDateStr?.let {
                try { LocalDate.parse(it) } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid targetDate format.")
                }
            } ?: LocalDate.now()
            val baseDate = baseDateStr?.let {
                try { LocalDate.parse(it) } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid baseDate format.")
                }
            } ?: targetDate.minusDays(1)

            val baseJob = jobRecorder.findLatestCompletedByDate(portfolioId, baseDate)
                ?: return@post call.respond(HttpStatusCode.NotFound, "No completed job for base date $baseDate")
            val targetJob = jobRecorder.findLatestCompletedByDate(portfolioId, targetDate)
                ?: return@post call.respond(HttpStatusCode.NotFound, "No completed job for target date $targetDate")

            val attribution = varAttributionService.attributeVaRChange(
                PortfolioId(portfolioId),
                baseJob,
                targetJob,
            )
            call.respond(attribution.toResponse())
        }
    }

    // Model comparison — runs two valuations with different parameters and compares
    if (riskEngineClient != null && positionProvider != null) {
        post("/api/v1/risk/compare/{portfolioId}/model") {
            val portfolioId = call.requirePathParam("portfolioId")
            val body = call.receive<ModelComparisonRequestBody>()

            val baseCalcType = CalculationType.valueOf(body.calculationType ?: "PARAMETRIC")
            val baseConfLevel = ConfidenceLevel.valueOf(body.confidenceLevel ?: "CL_95")
            val targetCalcType = CalculationType.valueOf(body.targetCalculationType ?: "MONTE_CARLO")
            val targetConfLevel = ConfidenceLevel.valueOf(body.targetConfidenceLevel ?: "CL_99")

            val positions = positionProvider.getPositions(PortfolioId(portfolioId))

            val baseRequest = VaRCalculationRequest(
                portfolioId = PortfolioId(portfolioId),
                calculationType = baseCalcType,
                confidenceLevel = baseConfLevel,
            )
            val targetRequest = VaRCalculationRequest(
                portfolioId = PortfolioId(portfolioId),
                calculationType = targetCalcType,
                confidenceLevel = targetConfLevel,
                numSimulations = body.targetNumSimulations ?: 10_000,
            )

            val baseResult = riskEngineClient.valuate(baseRequest, positions)
            val targetResult = riskEngineClient.valuate(targetRequest, positions)

            val today = LocalDate.now()
            val baseSnapshot = baseResult.toRunSnapshot("${baseCalcType.name} / ${baseConfLevel.name}", today)
            val targetSnapshot = targetResult.toRunSnapshot("${targetCalcType.name} / ${targetConfLevel.name}", today)

            val comparison = runComparisonService.compareSnapshots(
                baseSnapshot,
                targetSnapshot,
                ComparisonType.MODEL_GOVERNANCE,
                portfolioId,
            )
            call.respond(comparison.toResponse())
        }
    }

    // Lazy quantitative diff for a specific market data item between two manifests
    if (manifestRepo != null && blobStore != null && marketDataQuantDiffer != null) {
        get("/api/v1/risk/compare/{portfolioId}/market-data-quant") {
            val dataType = call.request.queryParameters["dataType"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "dataType is required"))
            val instrumentId = call.request.queryParameters["instrumentId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "instrumentId is required"))
            val baseManifestId = call.request.queryParameters["baseManifestId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "baseManifestId is required"))
            val targetManifestId = call.request.queryParameters["targetManifestId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "targetManifestId is required"))

            val baseUuid = try { UUID.fromString(baseManifestId) } catch (_: Exception) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid baseManifestId"))
            }
            val targetUuid = try { UUID.fromString(targetManifestId) } catch (_: Exception) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid targetManifestId"))
            }

            val baseRefs = manifestRepo.findMarketDataRefs(baseUuid)
            val targetRefs = manifestRepo.findMarketDataRefs(targetUuid)

            val baseRef = baseRefs.find { it.dataType == dataType && it.instrumentId == instrumentId }
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Base market data ref not found"))
            val targetRef = targetRefs.find { it.dataType == dataType && it.instrumentId == instrumentId }
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Target market data ref not found"))

            val basePayload = blobStore.get(baseRef.contentHash)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Base blob not found"))
            val targetPayload = blobStore.get(targetRef.contentHash)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Target blob not found"))

            val magnitude = marketDataQuantDiffer.computeMagnitude(dataType, basePayload, targetPayload)
            call.respond(MarketDataQuantDiffResponse(
                dataType = dataType,
                instrumentId = instrumentId,
                magnitude = magnitude.name,
            ))
        }
    }
}
