package com.kinetix.risk.service

import com.kinetix.risk.model.InputChangeSummary
import com.kinetix.risk.model.MarketDataInputChange
import com.kinetix.risk.model.MarketDataInputChangeType
import com.kinetix.risk.model.MarketDataRef
import com.kinetix.risk.model.MarketDataSnapshotStatus
import com.kinetix.risk.model.PositionInputChange
import com.kinetix.risk.model.PositionInputChangeType
import com.kinetix.risk.model.PositionSnapshotEntry
import com.kinetix.risk.model.RunManifest
import java.math.BigDecimal

class InputChangeDiffer {

    fun computeInputChanges(
        baseManifest: RunManifest,
        targetManifest: RunManifest,
        basePositions: List<PositionSnapshotEntry>,
        targetPositions: List<PositionSnapshotEntry>,
        baseMarketDataRefs: List<MarketDataRef>,
        targetMarketDataRefs: List<MarketDataRef>,
    ): InputChangeSummary {
        val positionsChanged = baseManifest.positionDigest != targetManifest.positionDigest
        val marketDataChanged = baseManifest.marketDataDigest != targetManifest.marketDataDigest
        val modelVersionChanged = baseManifest.modelVersion != targetManifest.modelVersion

        val positionChanges = if (positionsChanged) {
            diffPositions(basePositions, targetPositions)
        } else {
            emptyList()
        }

        val marketDataChanges = if (marketDataChanged) {
            diffMarketData(baseMarketDataRefs, targetMarketDataRefs)
        } else {
            emptyList()
        }

        return InputChangeSummary(
            positionsChanged = positionsChanged,
            marketDataChanged = marketDataChanged,
            modelVersionChanged = modelVersionChanged,
            baseModelVersion = baseManifest.modelVersion,
            targetModelVersion = targetManifest.modelVersion,
            positionChanges = positionChanges,
            marketDataChanges = marketDataChanges,
        )
    }

    private fun diffPositions(
        basePositions: List<PositionSnapshotEntry>,
        targetPositions: List<PositionSnapshotEntry>,
    ): List<PositionInputChange> {
        val baseByInstrument = basePositions.associateBy { it.instrumentId }
        val targetByInstrument = targetPositions.associateBy { it.instrumentId }
        val allInstrumentIds = baseByInstrument.keys + targetByInstrument.keys

        val changes = allInstrumentIds.mapNotNull { instrumentId ->
            val base = baseByInstrument[instrumentId]
            val target = targetByInstrument[instrumentId]
            classifyPositionChange(instrumentId, base, target)
        }

        return changes.sortedByDescending { change ->
            change.quantityDelta?.abs() ?: BigDecimal.ZERO
        }
    }

    private fun classifyPositionChange(
        instrumentId: String,
        base: PositionSnapshotEntry?,
        target: PositionSnapshotEntry?,
    ): PositionInputChange? {
        if (base == null && target == null) return null

        if (base == null && target != null) {
            return PositionInputChange(
                instrumentId = instrumentId,
                assetClass = target.assetClass,
                changeType = PositionInputChangeType.ADDED,
                baseQuantity = null,
                targetQuantity = target.quantity,
                quantityDelta = target.quantity,
                baseMarketPrice = null,
                targetMarketPrice = target.marketPriceAmount,
                priceDelta = null,
                currency = target.currency,
            )
        }

        if (base != null && target == null) {
            return PositionInputChange(
                instrumentId = instrumentId,
                assetClass = base.assetClass,
                changeType = PositionInputChangeType.REMOVED,
                baseQuantity = base.quantity,
                targetQuantity = null,
                quantityDelta = base.quantity.negate(),
                baseMarketPrice = base.marketPriceAmount,
                targetMarketPrice = null,
                priceDelta = null,
                currency = base.currency,
            )
        }

        // Both present: check what changed
        base!!
        target!!
        val quantityChanged = base.quantity.compareTo(target.quantity) != 0
        val priceChanged = base.marketPriceAmount.compareTo(target.marketPriceAmount) != 0

        if (!quantityChanged && !priceChanged) return null

        val changeType = when {
            quantityChanged && priceChanged -> PositionInputChangeType.BOTH_CHANGED
            quantityChanged -> PositionInputChangeType.QUANTITY_CHANGED
            else -> PositionInputChangeType.PRICE_CHANGED
        }

        return PositionInputChange(
            instrumentId = instrumentId,
            assetClass = base.assetClass,
            changeType = changeType,
            baseQuantity = base.quantity,
            targetQuantity = target.quantity,
            quantityDelta = if (quantityChanged) target.quantity - base.quantity else null,
            baseMarketPrice = base.marketPriceAmount,
            targetMarketPrice = target.marketPriceAmount,
            priceDelta = if (priceChanged) target.marketPriceAmount - base.marketPriceAmount else null,
            currency = base.currency,
        )
    }

    private fun diffMarketData(
        baseRefs: List<MarketDataRef>,
        targetRefs: List<MarketDataRef>,
    ): List<MarketDataInputChange> {
        val baseByKey = baseRefs.associateBy { marketDataKey(it) }
        val targetByKey = targetRefs.associateBy { marketDataKey(it) }
        val allKeys = baseByKey.keys + targetByKey.keys

        return allKeys.mapNotNull { key ->
            val base = baseByKey[key]
            val target = targetByKey[key]
            classifyMarketDataChange(base, target)
        }
    }

    private fun classifyMarketDataChange(
        base: MarketDataRef?,
        target: MarketDataRef?,
    ): MarketDataInputChange? {
        if (base == null && target == null) return null

        if (base == null && target != null) {
            return MarketDataInputChange(
                dataType = target.dataType,
                instrumentId = target.instrumentId,
                assetClass = target.assetClass,
                changeType = MarketDataInputChangeType.ADDED,
                baseContentHash = null,
                targetContentHash = target.contentHash.ifEmpty { null },
            )
        }

        if (base != null && target == null) {
            return MarketDataInputChange(
                dataType = base.dataType,
                instrumentId = base.instrumentId,
                assetClass = base.assetClass,
                changeType = MarketDataInputChangeType.REMOVED,
                baseContentHash = base.contentHash.ifEmpty { null },
                targetContentHash = null,
            )
        }

        base!!
        target!!
        val baseMissing = base.status == MarketDataSnapshotStatus.MISSING || base.contentHash.isEmpty()
        val targetMissing = target.status == MarketDataSnapshotStatus.MISSING || target.contentHash.isEmpty()

        return when {
            baseMissing && !targetMissing -> MarketDataInputChange(
                dataType = base.dataType,
                instrumentId = base.instrumentId,
                assetClass = base.assetClass,
                changeType = MarketDataInputChangeType.BECAME_AVAILABLE,
                baseContentHash = null,
                targetContentHash = target.contentHash,
            )
            !baseMissing && targetMissing -> MarketDataInputChange(
                dataType = base.dataType,
                instrumentId = base.instrumentId,
                assetClass = base.assetClass,
                changeType = MarketDataInputChangeType.BECAME_MISSING,
                baseContentHash = base.contentHash,
                targetContentHash = null,
            )
            base.contentHash != target.contentHash -> MarketDataInputChange(
                dataType = base.dataType,
                instrumentId = base.instrumentId,
                assetClass = base.assetClass,
                changeType = MarketDataInputChangeType.CHANGED,
                baseContentHash = base.contentHash,
                targetContentHash = target.contentHash,
            )
            else -> null
        }
    }

    private fun marketDataKey(ref: MarketDataRef): String = "${ref.dataType}:${ref.instrumentId}"
}
