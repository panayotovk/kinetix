package com.kinetix.notification.engine.extractors

import com.kinetix.common.kafka.events.RiskResultEvent
import com.kinetix.notification.model.AlertType

interface MetricExtractor {
    val type: AlertType
    fun extract(event: RiskResultEvent): Double?
}

class VarBreachExtractor : MetricExtractor {
    override val type = AlertType.VAR_BREACH
    override fun extract(event: RiskResultEvent) = event.varValue.toDoubleOrNull()
}

class PnlThresholdExtractor : MetricExtractor {
    override val type = AlertType.PNL_THRESHOLD
    override fun extract(event: RiskResultEvent) = event.expectedShortfall.toDoubleOrNull()
}

class RiskLimitExtractor : MetricExtractor {
    override val type = AlertType.RISK_LIMIT
    override fun extract(event: RiskResultEvent) = event.varValue.toDoubleOrNull()
}

class DeltaBreachExtractor : MetricExtractor {
    override val type = AlertType.DELTA_BREACH
    override fun extract(event: RiskResultEvent) = event.aggregateDelta?.toDoubleOrNull()
}

class VegaBreachExtractor : MetricExtractor {
    override val type = AlertType.VEGA_BREACH
    override fun extract(event: RiskResultEvent) = event.aggregateVega?.toDoubleOrNull()
}

class ConcentrationExtractor : MetricExtractor {
    override val type = AlertType.CONCENTRATION
    override fun extract(event: RiskResultEvent): Double? {
        val items = event.concentrationByInstrument ?: return null
        return items.maxByOrNull { it.percentage }?.percentage
    }
}

class MarginBreachExtractor : MetricExtractor {
    override val type = AlertType.MARGIN_BREACH
    override fun extract(event: RiskResultEvent) = event.marginUtilisation
}

class DataStalenessExtractor : MetricExtractor {
    override val type = AlertType.DATA_STALENESS
    override fun extract(event: RiskResultEvent): Double? = null
}

val DEFAULT_EXTRACTORS: List<MetricExtractor> = listOf(
    VarBreachExtractor(),
    PnlThresholdExtractor(),
    RiskLimitExtractor(),
    DeltaBreachExtractor(),
    VegaBreachExtractor(),
    ConcentrationExtractor(),
    MarginBreachExtractor(),
    DataStalenessExtractor(),
)
