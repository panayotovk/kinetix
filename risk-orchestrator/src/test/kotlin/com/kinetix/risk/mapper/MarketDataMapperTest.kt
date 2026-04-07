package com.kinetix.risk.mapper

import com.kinetix.proto.risk.MarketDataType
import com.kinetix.risk.model.MatrixMarketData
import com.kinetix.risk.model.ScalarMarketData
import com.kinetix.risk.model.TimeSeriesMarketData
import com.kinetix.risk.model.TimeSeriesPoint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class MarketDataMapperTest : FunSpec({

    test("maps scalar market data to proto") {
        val scalar = ScalarMarketData(
            dataType = "SPOT_PRICE",
            instrumentId = "AAPL",
            assetClass = "EQUITY",
            value = 170.50,
        )

        val proto = scalar.toProto()

        proto.dataType shouldBe MarketDataType.SPOT_PRICE
        proto.instrumentId shouldBe "AAPL"
        proto.assetClass shouldBe "EQUITY"
        proto.scalar shouldBe 170.50
    }

    test("maps time series market data to proto") {
        val ts = TimeSeriesMarketData(
            dataType = "HISTORICAL_PRICES",
            instrumentId = "AAPL",
            assetClass = "EQUITY",
            points = listOf(
                TimeSeriesPoint(Instant.ofEpochSecond(1000000), 100.0),
                TimeSeriesPoint(Instant.ofEpochSecond(1086400), 101.5),
            ),
        )

        val proto = ts.toProto()

        proto.dataType shouldBe MarketDataType.HISTORICAL_PRICES
        proto.instrumentId shouldBe "AAPL"
        proto.timeSeries.pointsCount shouldBe 2
        proto.timeSeries.getPoints(0).timestamp.seconds shouldBe 1000000L
        proto.timeSeries.getPoints(0).value shouldBe 100.0
        proto.timeSeries.getPoints(1).value shouldBe 101.5
    }

    test("maps matrix market data with row and column labels") {
        val matrix = MatrixMarketData(
            dataType = "VOLATILITY_SURFACE",
            instrumentId = "AAPL",
            assetClass = "EQUITY",
            rows = listOf("30", "60"),
            columns = listOf("100.0", "150.0"),
            values = listOf(0.20, 0.25, 0.18, 0.22),
        )

        val proto = matrix.toProto()

        proto.dataType shouldBe MarketDataType.VOLATILITY_SURFACE
        proto.instrumentId shouldBe "AAPL"
        proto.matrix.rows shouldBe 2
        proto.matrix.cols shouldBe 2
        proto.matrix.valuesCount shouldBe 4
        proto.matrix.labelsCount shouldBe 4
        proto.matrix.getLabelsList() shouldBe listOf("30", "60", "100.0", "150.0")
    }

    test("maps unknown data type to UNSPECIFIED") {
        val scalar = ScalarMarketData(
            dataType = "UNKNOWN_TYPE",
            instrumentId = "X",
            assetClass = "EQUITY",
            value = 1.0,
        )

        val proto = scalar.toProto()

        proto.dataType shouldBe MarketDataType.MARKET_DATA_TYPE_UNSPECIFIED
    }
})
