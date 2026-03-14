package com.kinetix.risk.routes

import com.kinetix.common.model.PortfolioId
import com.kinetix.risk.cache.QuantDiffCache
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
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.util.UUID

private const val MAX_BLOB_SIZE_BYTES = 1_048_576 // 1 MB
private const val QUANT_DIFF_TIMEOUT_MS = 30_000L

fun Route.runComparisonRoutes(
    runComparisonService: RunComparisonService,
    jobRecorder: ValuationJobRecorder,
    varAttributionService: VaRAttributionService? = null,
    riskEngineClient: RiskEngineClient? = null,
    positionProvider: PositionProvider? = null,
    manifestRepo: RunManifestRepository? = null,
    blobStore: MarketDataBlobStore? = null,
    marketDataQuantDiffer: MarketDataQuantDiffer? = null,
    quantDiffCache: QuantDiffCache? = null,
    meterRegistry: MeterRegistry = SimpleMeterRegistry(),
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
        val quantDiffSemaphore = Semaphore(2)

        get("/api/v1/risk/compare/{portfolioId}/market-data-quant") {
            val sample = Timer.start(meterRegistry)
            meterRegistry.counter("manifest.diff.requests.total").increment()

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

            // Rate limit: max 2 concurrent diff computations
            if (!quantDiffSemaphore.tryAcquire()) {
                meterRegistry.counter("manifest.diff.rejected.total", "reason", "concurrency").increment()
                return@get call.respond(
                    HttpStatusCode.TooManyRequests,
                    mapOf("error" to "Too many concurrent diff requests, please retry"),
                )
            }

            try {
                val baseRefs = manifestRepo.findMarketDataRefs(baseUuid)
                val targetRefs = manifestRepo.findMarketDataRefs(targetUuid)

                val baseRef = baseRefs.find { it.dataType == dataType && it.instrumentId == instrumentId }
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Base market data ref not found"))
                val targetRef = targetRefs.find { it.dataType == dataType && it.instrumentId == instrumentId }
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Target market data ref not found"))

                // Check cache first (content-addressed, so hash pair is a stable key)
                val cached = quantDiffCache?.get(baseRef.contentHash, targetRef.contentHash)
                if (cached != null) {
                    meterRegistry.counter("manifest.diff.cache.hits.total").increment()
                    sample.stop(meterRegistry.timer("manifest.diff.duration.seconds"))
                    return@get call.respond(MarketDataQuantDiffResponse(
                        dataType = dataType,
                        instrumentId = instrumentId,
                        magnitude = cached.name,
                    ))
                }

                val basePayload = blobStore.get(baseRef.contentHash)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Base blob not found"))
                val targetPayload = blobStore.get(targetRef.contentHash)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Target blob not found"))

                // Memory protection: reject blobs larger than 1 MB
                meterRegistry.summary("manifest.blob.size.bytes").record(basePayload.length.toDouble())
                meterRegistry.summary("manifest.blob.size.bytes").record(targetPayload.length.toDouble())
                if (basePayload.length > MAX_BLOB_SIZE_BYTES || targetPayload.length > MAX_BLOB_SIZE_BYTES) {
                    meterRegistry.counter("manifest.diff.rejected.total", "reason", "blob_too_large").increment()
                    return@get call.respond(
                        HttpStatusCode.UnprocessableEntity,
                        mapOf("error" to "Blob payload exceeds size limit (${MAX_BLOB_SIZE_BYTES / 1024}KB)"),
                    )
                }

                // Timeout protection for deserialization and computation
                val result = withTimeout(QUANT_DIFF_TIMEOUT_MS) {
                    marketDataQuantDiffer.computeQuantDiff(dataType, basePayload, targetPayload)
                }

                // Cache the result
                quantDiffCache?.put(baseRef.contentHash, targetRef.contentHash, result.magnitude)

                sample.stop(meterRegistry.timer("manifest.diff.duration.seconds"))
                call.respond(MarketDataQuantDiffResponse(
                    dataType = dataType,
                    instrumentId = instrumentId,
                    magnitude = result.magnitude.name,
                    summary = result.summary,
                    caveats = result.caveats,
                ))
            } finally {
                quantDiffSemaphore.release()
            }
        }
    }
}
