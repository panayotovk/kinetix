package com.kinetix.risk.service

import com.kinetix.common.model.PortfolioId
import com.kinetix.risk.client.RiskEngineClient
import com.kinetix.risk.model.*
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
) {
    private val logger = LoggerFactory.getLogger(ReplayService::class.java)

    suspend fun replay(jobId: UUID): ReplayResult {
        // 1. Load the manifest
        val manifest = manifestRepo.findByJobId(jobId)
            ?: return ReplayResult.ManifestNotFound

        // 2. Load the position snapshot
        val positionEntries = manifestRepo.findPositionSnapshot(manifest.manifestId)
        if (positionEntries.isEmpty()) {
            return ReplayResult.Error("No position snapshot found for manifest ${manifest.manifestId}")
        }

        // 3. Load market data refs and resolve blobs
        val marketDataRefs = manifestRepo.findMarketDataRefs(manifest.manifestId)
        val marketDataValues = mutableListOf<MarketDataValue>()
        for (ref in marketDataRefs) {
            if (ref.status == MarketDataSnapshotStatus.MISSING) continue
            val blob = blobStore.get(ref.contentHash)
            if (blob == null) {
                logger.warn("Market data blob {} not found for manifest {}", ref.contentHash, manifest.manifestId)
                continue
            }
            val value = deserializeMarketData(blob)
            if (value != null) {
                marketDataValues.add(value)
            }
        }

        // 4. Build the replay request matching original parameters
        val replayRequest = VaRCalculationRequest(
            portfolioId = PortfolioId(manifest.portfolioId),
            calculationType = CalculationType.valueOf(manifest.calculationType),
            confidenceLevel = ConfidenceLevel.valueOf(manifest.confidenceLevel),
            timeHorizonDays = manifest.timeHorizonDays,
            numSimulations = manifest.numSimulations,
            monteCarloSeed = manifest.monteCarloSeed,
        )

        // 5. Create replay positions from snapshot
        val replayProvider = ReplayPositionProvider(positionEntries, manifest.portfolioId)
        val positions = replayProvider.getPositions(PortfolioId(manifest.portfolioId))

        // 6. Re-run the valuation
        logger.info("Replaying job {} with manifest {} ({} positions, {} market data items)",
            jobId, manifest.manifestId, positions.size, marketDataValues.size)

        val result = riskEngineClient.valuate(replayRequest, positions, marketDataValues)

        // 7. Verify reproducibility by comparing input digest
        val replayPositionDigest = InputDigest.digestPositions(positions)
        val replayMarketDataDigest = InputDigest.digestMarketData(marketDataValues)
        val replayInputDigest = InputDigest.digestInputs(
            replayRequest, replayPositionDigest, replayMarketDataDigest,
            manifest.modelVersion,
        )

        val digestMatch = replayInputDigest == manifest.inputDigest

        logger.info(
            "Replay complete for job {}: digest match={}, original VaR={}, replay VaR={}",
            jobId, digestMatch,
            jobRecorder.findByJobId(jobId)?.varValue,
            result.varValue,
        )

        return ReplayResult.Success(
            manifest = manifest,
            replayResult = result,
            inputDigestMatch = digestMatch,
            originalInputDigest = manifest.inputDigest,
            replayInputDigest = replayInputDigest,
        )
    }

    suspend fun getManifest(jobId: UUID): RunManifest? =
        manifestRepo.findByJobId(jobId)

    private fun deserializeMarketData(blob: String): MarketDataValue? {
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
            logger.warn("Failed to deserialize market data blob", e)
            null
        }
    }
}
