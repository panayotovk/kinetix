package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ValuationResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class ValuationResultResponse(
    val bookId: String,
    val calculationType: String,
    val confidenceLevel: String,
    val varValue: String,
    val expectedShortfall: String,
    val componentBreakdown: List<ComponentBreakdownDto>,
    val calculatedAt: String,
    val greeks: GreeksResponse? = null,
    val computedOutputs: List<String>? = null,
    val pvValue: String? = null,
    val valuationDate: String? = null,
)

fun ValuationResultSummary.toResponse(): ValuationResultResponse = ValuationResultResponse(
    bookId = bookId,
    calculationType = calculationType,
    confidenceLevel = confidenceLevel,
    varValue = "%.2f".format(varValue),
    expectedShortfall = "%.2f".format(expectedShortfall),
    componentBreakdown = componentBreakdown.map { it.toDto() },
    calculatedAt = calculatedAt.toString(),
    greeks = greeks?.toResponse(),
    pvValue = pvValue?.let { "%.2f".format(it) },
    valuationDate = valuationDate,
)
