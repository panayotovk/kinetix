package com.kinetix.risk.service

import com.kinetix.common.model.BookId
import com.kinetix.risk.client.RiskEngineClient
import com.kinetix.risk.model.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class ReplayService(
    private val manifestRepo: RunManifestRepository,
    private val blobStore: MarketDataBlobStore,
    private val riskEngineClient: RiskEngineClient,
    private val jobRecorder: ValuationJobRecorder,
    private val auditPublisher: RiskAuditEventPublisher? = null,
    private val replayRunRepo: ReplayRunRepository? = null,
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry(),
) {
    private val logger = LoggerFactory.getLogger(ReplayService::class.java)

    suspend fun replay(jobId: UUID): ReplayResult {
        val sample = io.micrometer.core.instrument.Timer.start(meterRegistry)

        // 1. Load the manifest
        val manifest = manifestRepo.findByJobId(jobId)
        if (manifest == null) {
            meterRegistry.counter("replay.requests", "result", "manifest_not_found").increment()
            return ReplayResult.ManifestNotFound
        }

        // 2. Load the position snapshot
        val positionEntries = manifestRepo.findPositionSnapshot(manifest.manifestId)
        if (positionEntries.isEmpty()) {
            meterRegistry.counter("replay.requests", "result", "error").increment()
            return ReplayResult.Error("No position snapshot found for manifest ${manifest.manifestId}")
        }

        // 3. Load market data refs and resolve blobs
        val marketDataRefs = manifestRepo.findMarketDataRefs(manifest.manifestId)
        val marketDataValues = mutableListOf<MarketDataValue>()
        for (ref in marketDataRefs) {
            if (ref.status == MarketDataSnapshotStatus.MISSING) continue
            val blob = blobStore.get(ref.contentHash)
            if (blob == null) {
                logger.error(
                    "Blob resolution failed during replay of job {}: contentHash={}, dataType={}, instrumentId={}",
                    jobId, ref.contentHash, ref.dataType, ref.instrumentId,
                )
                meterRegistry.counter(
                    "replay.blob_misses", "data_type", ref.dataType, "source_service", ref.sourceService,
                ).increment()
                meterRegistry.counter("replay.requests", "result", "blob_missing").increment()
                return ReplayResult.BlobMissing(
                    manifestId = manifest.manifestId.toString(),
                    contentHash = ref.contentHash,
                    dataType = ref.dataType,
                    instrumentId = ref.instrumentId,
                )
            }
            val value = deserializeMarketData(blob, ref.contentHash)
            if (value != null) {
                marketDataValues.add(value)
            }
        }

        // 4. Build the replay request matching original parameters
        val replayRequest = VaRCalculationRequest(
            bookId = BookId(manifest.bookId),
            calculationType = CalculationType.valueOf(manifest.calculationType),
            confidenceLevel = ConfidenceLevel.valueOf(manifest.confidenceLevel),
            timeHorizonDays = manifest.timeHorizonDays,
            numSimulations = manifest.numSimulations,
            monteCarloSeed = manifest.monteCarloSeed,
        )

        // 5. Create replay positions from snapshot
        val replayProvider = ReplayPositionProvider(positionEntries, manifest.bookId)
        val positions = replayProvider.getPositions(BookId(manifest.bookId))

        // 6. Re-run the valuation
        logger.info(
            "Replaying job {} with manifest {} ({} positions, {} market data items)",
            jobId, manifest.manifestId, positions.size, marketDataValues.size,
        )

        val result = riskEngineClient.valuate(replayRequest, positions, marketDataValues)

        // 7. Verify reproducibility by comparing input digest
        val replayPositionDigest = InputDigest.digestPositions(positions)
        val replayMarketDataDigest = InputDigest.digestMarketData(marketDataValues)
        val replayInputDigest = InputDigest.digestInputs(
            replayRequest, replayPositionDigest, replayMarketDataDigest,
            manifest.modelVersion,
        )

        val digestMatch = replayInputDigest == manifest.inputDigest

        // 8. Get original values for comparison
        val originalJob = jobRecorder.findByJobId(jobId)
        val originalVarValue = manifest.varValue ?: originalJob?.varValue
        val originalExpectedShortfall = manifest.expectedShortfall ?: originalJob?.expectedShortfall

        // 9. Record metrics
        sample.stop(meterRegistry.timer("replay.duration", "book_id", manifest.bookId))
        meterRegistry.counter("replay.requests", "result", "success").increment()
        meterRegistry.counter(
            "replay.digest_match", "matched", digestMatch.toString(), "book_id", manifest.bookId,
        ).increment()

        logger.info(
            "Replay complete for job {}: digest match={}, original VaR={}, replay VaR={}",
            jobId, digestMatch, originalVarValue, result.varValue,
        )

        val replayOutputDigest = InputDigest.digestOutputs(result.varValue, result.expectedShortfall)

        val replayResult = ReplayResult.Success(
            manifest = manifest,
            replayResult = result,
            inputDigestMatch = digestMatch,
            originalInputDigest = manifest.inputDigest,
            replayInputDigest = replayInputDigest,
            originalVarValue = originalVarValue,
            originalExpectedShortfall = originalExpectedShortfall,
        )

        // 10. Persist replay run
        val replayedAt = Instant.now()
        try {
            replayRunRepo?.save(
                ReplayRun(
                    replayId = UUID.randomUUID(),
                    manifestId = manifest.manifestId,
                    originalJobId = jobId,
                    replayedAt = replayedAt,
                    triggeredBy = "system",
                    replayVarValue = result.varValue,
                    replayExpectedShortfall = result.expectedShortfall,
                    replayModelVersion = result.modelVersion,
                    replayOutputDigest = replayOutputDigest,
                    originalVarValue = originalVarValue,
                    originalExpectedShortfall = originalExpectedShortfall,
                    inputDigestMatch = digestMatch,
                    originalInputDigest = manifest.inputDigest,
                    replayInputDigest = replayInputDigest,
                )
            )
        } catch (e: Exception) {
            logger.warn("Failed to persist replay run for job {}", jobId, e)
        }

        // 11. Emit audit event
        try {
            auditPublisher?.publish(
                RunReplayedAuditEvent(
                    jobId = jobId.toString(),
                    bookId = manifest.bookId,
                    valuationDate = manifest.valuationDate.toString(),
                    manifestId = manifest.manifestId.toString(),
                    replayedAt = replayedAt.toString(),
                    inputDigestMatch = digestMatch,
                    originalVarValue = originalVarValue,
                    replayVarValue = result.varValue,
                    originalExpectedShortfall = originalExpectedShortfall,
                    replayExpectedShortfall = result.expectedShortfall,
                    replayModelVersion = result.modelVersion,
                )
            )
        } catch (e: Exception) {
            logger.warn("Failed to publish RISK_RUN_REPLAYED audit event for job {}", jobId, e)
        }

        return replayResult
    }

    suspend fun getManifest(jobId: UUID): RunManifest? =
        manifestRepo.findByJobId(jobId)

    private fun deserializeMarketData(blob: String, contentHash: String): MarketDataValue? {
        return try {
            val obj = Json.parseToJsonElement(blob).jsonObject
            val dataType = obj["dataType"]?.jsonPrimitive?.content ?: return null
            val instrumentId = obj["instrumentId"]?.jsonPrimitive?.content ?: return null
            val assetClass = obj["assetClass"]?.jsonPrimitive?.content ?: return null

            when {
                obj.containsKey("value") -> ScalarMarketData(
                    dataType = dataType,
                    instrumentId = instrumentId,
                    assetClass = assetClass,
                    value = obj["value"]!!.jsonPrimitive.double,
                )
                obj.containsKey("points") -> {
                    val points = obj["points"]!!.jsonArray
                    if (points.firstOrNull()?.jsonObject?.containsKey("tenor") == true) {
                        CurveMarketData(
                            dataType = dataType,
                            instrumentId = instrumentId,
                            assetClass = assetClass,
                            points = points.map { p ->
                                val pObj = p.jsonObject
                                CurvePointValue(
                                    tenor = pObj["tenor"]!!.jsonPrimitive.content,
                                    value = pObj["value"]!!.jsonPrimitive.double,
                                )
                            },
                        )
                    } else {
                        TimeSeriesMarketData(
                            dataType = dataType,
                            instrumentId = instrumentId,
                            assetClass = assetClass,
                            points = points.map { p ->
                                val pObj = p.jsonObject
                                TimeSeriesPoint(
                                    timestamp = Instant.parse(pObj["timestamp"]!!.jsonPrimitive.content),
                                    value = pObj["value"]!!.jsonPrimitive.double,
                                )
                            },
                        )
                    }
                }
                obj.containsKey("rows") -> MatrixMarketData(
                    dataType = dataType,
                    instrumentId = instrumentId,
                    assetClass = assetClass,
                    rows = obj["rows"]!!.jsonArray.map { it.jsonPrimitive.content },
                    columns = obj["columns"]!!.jsonArray.map { it.jsonPrimitive.content },
                    values = obj["values"]!!.jsonArray.map { it.jsonPrimitive.double },
                )
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Failed to deserialize market data blob {}", contentHash, e)
            null
        }
    }
}
