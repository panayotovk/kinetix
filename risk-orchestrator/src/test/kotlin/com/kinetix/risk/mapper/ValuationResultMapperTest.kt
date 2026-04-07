package com.kinetix.risk.mapper

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.BookId
import com.kinetix.risk.model.CalculationType
import com.kinetix.risk.model.ConfidenceLevel
import com.kinetix.risk.model.ValuationOutput
import com.google.protobuf.Timestamp
import com.kinetix.proto.common.AssetClass as ProtoAssetClass
import com.kinetix.proto.risk.ConfidenceLevel as ProtoConfidenceLevel
import com.kinetix.proto.risk.GreekValues as ProtoGreekValues
import com.kinetix.proto.risk.GreeksSummary
import com.kinetix.proto.risk.PositionGreek as ProtoPositionGreek
import com.kinetix.proto.risk.RiskCalculationType
import com.kinetix.proto.risk.VaRComponentBreakdown
import com.kinetix.proto.risk.ValuationResponse
import com.kinetix.proto.risk.ValuationOutput as ProtoValuationOutput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ValuationResultMapperTest : FunSpec({

    test("maps ValuationResponse with VaR and Greeks to domain") {
        val now = Timestamp.newBuilder().setSeconds(1700000000).build()
        val response = ValuationResponse.newBuilder()
            .setBookId(com.kinetix.proto.common.BookId.newBuilder().setValue("port-1"))
            .setCalculationType(RiskCalculationType.PARAMETRIC)
            .setConfidenceLevel(ProtoConfidenceLevel.CL_95)
            .setVarValue(25000.0)
            .setExpectedShortfall(31000.0)
            .addComponentBreakdown(
                VaRComponentBreakdown.newBuilder()
                    .setAssetClass(ProtoAssetClass.EQUITY)
                    .setVarContribution(15000.0)
                    .setPercentageOfTotal(60.0)
            )
            .setGreeks(
                GreeksSummary.newBuilder()
                    .addAssetClassGreeks(
                        ProtoGreekValues.newBuilder()
                            .setAssetClass(ProtoAssetClass.EQUITY)
                            .setDelta(1234.5)
                            .setGamma(56.7)
                            .setVega(890.1)
                    )
                    .setTheta(-42.0)
                    .setRho(15.3)
            )
            .addComputedOutputs(ProtoValuationOutput.VAR)
            .addComputedOutputs(ProtoValuationOutput.EXPECTED_SHORTFALL)
            .addComputedOutputs(ProtoValuationOutput.GREEKS)
            .setCalculatedAt(now)
            .build()

        val result = response.toDomainValuation()

        result.bookId shouldBe BookId("port-1")
        result.calculationType shouldBe CalculationType.PARAMETRIC
        result.confidenceLevel shouldBe ConfidenceLevel.CL_95
        result.varValue shouldBe 25000.0
        result.expectedShortfall shouldBe 31000.0
        result.componentBreakdown shouldHaveSize 1
        result.componentBreakdown[0].assetClass shouldBe AssetClass.EQUITY

        result.greeks.shouldNotBeNull()
        result.greeks!!.assetClassGreeks shouldHaveSize 1
        result.greeks!!.assetClassGreeks[0].assetClass shouldBe AssetClass.EQUITY
        result.greeks!!.assetClassGreeks[0].delta shouldBe 1234.5
        result.greeks!!.assetClassGreeks[0].gamma shouldBe 56.7
        result.greeks!!.assetClassGreeks[0].vega shouldBe 890.1
        result.greeks!!.theta shouldBe -42.0
        result.greeks!!.rho shouldBe 15.3

        result.computedOutputs shouldContainExactlyInAnyOrder listOf(
            ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL, ValuationOutput.GREEKS,
        )
        result.calculatedAt.epochSecond shouldBe 1700000000
        result.pvValue.shouldBeNull()
    }

    test("maps ValuationResponse without Greeks") {
        val now = Timestamp.newBuilder().setSeconds(1700000000).build()
        val response = ValuationResponse.newBuilder()
            .setBookId(com.kinetix.proto.common.BookId.newBuilder().setValue("port-2"))
            .setCalculationType(RiskCalculationType.HISTORICAL)
            .setConfidenceLevel(ProtoConfidenceLevel.CL_99)
            .setVarValue(10000.0)
            .setExpectedShortfall(12000.0)
            .addComputedOutputs(ProtoValuationOutput.VAR)
            .addComputedOutputs(ProtoValuationOutput.EXPECTED_SHORTFALL)
            .setCalculatedAt(now)
            .build()

        val result = response.toDomainValuation()

        result.bookId shouldBe BookId("port-2")
        result.varValue shouldBe 10000.0
        result.greeks.shouldBeNull()
        result.computedOutputs shouldContainExactlyInAnyOrder listOf(
            ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL,
        )
    }

    test("maps PV value from response to domain") {
        val now = Timestamp.newBuilder().setSeconds(1700000000).build()
        val response = ValuationResponse.newBuilder()
            .setBookId(com.kinetix.proto.common.BookId.newBuilder().setValue("port-pv"))
            .setCalculationType(RiskCalculationType.PARAMETRIC)
            .setConfidenceLevel(ProtoConfidenceLevel.CL_95)
            .setVarValue(25000.0)
            .setExpectedShortfall(31000.0)
            .setPvValue(1800000.0)
            .addComputedOutputs(ProtoValuationOutput.VAR)
            .addComputedOutputs(ProtoValuationOutput.EXPECTED_SHORTFALL)
            .addComputedOutputs(ProtoValuationOutput.PV)
            .setCalculatedAt(now)
            .build()

        val result = response.toDomainValuation()

        result.pvValue shouldBe 1800000.0
        result.computedOutputs shouldContainExactlyInAnyOrder listOf(
            ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL, ValuationOutput.PV,
        )
    }

    test("maps all asset classes in Greeks") {
        val now = Timestamp.newBuilder().setSeconds(1000).build()
        val response = ValuationResponse.newBuilder()
            .setBookId(com.kinetix.proto.common.BookId.newBuilder().setValue("p"))
            .setCalculationType(RiskCalculationType.PARAMETRIC)
            .setConfidenceLevel(ProtoConfidenceLevel.CL_95)
            .setVarValue(1.0)
            .setExpectedShortfall(2.0)
            .setGreeks(
                GreeksSummary.newBuilder()
                    .addAssetClassGreeks(
                        ProtoGreekValues.newBuilder()
                            .setAssetClass(ProtoAssetClass.EQUITY).setDelta(1.0).setGamma(2.0).setVega(3.0)
                    )
                    .addAssetClassGreeks(
                        ProtoGreekValues.newBuilder()
                            .setAssetClass(ProtoAssetClass.FIXED_INCOME).setDelta(4.0).setGamma(5.0).setVega(6.0)
                    )
                    .setTheta(-1.0)
                    .setRho(0.5)
            )
            .addComputedOutputs(ProtoValuationOutput.GREEKS)
            .setCalculatedAt(now)
            .build()

        val result = response.toDomainValuation()
        result.greeks.shouldNotBeNull()
        result.greeks!!.assetClassGreeks shouldHaveSize 2
        result.greeks!!.assetClassGreeks[0].assetClass shouldBe AssetClass.EQUITY
        result.greeks!!.assetClassGreeks[1].assetClass shouldBe AssetClass.FIXED_INCOME
    }

    test("maps position_greeks from proto response to domain positionGreeks") {
        val now = Timestamp.newBuilder().setSeconds(1700000000).build()
        val response = ValuationResponse.newBuilder()
            .setBookId(com.kinetix.proto.common.BookId.newBuilder().setValue("port-opts"))
            .setCalculationType(RiskCalculationType.PARAMETRIC)
            .setConfidenceLevel(ProtoConfidenceLevel.CL_95)
            .setVarValue(10000.0)
            .setExpectedShortfall(13000.0)
            .addPositionGreeks(
                ProtoPositionGreek.newBuilder()
                    .setInstrumentId("AAPL-OPT-JAN25-150C")
                    .setDelta(0.65)
                    .setGamma(0.03)
                    .setVega(125.4)
                    .setTheta(-8.2)
                    .setRho(12.1)
            )
            .addPositionGreeks(
                ProtoPositionGreek.newBuilder()
                    .setInstrumentId("MSFT-OPT-FEB25-400P")
                    .setDelta(-0.35)
                    .setGamma(0.02)
                    .setVega(98.7)
                    .setTheta(-5.6)
                    .setRho(-7.3)
            )
            .addComputedOutputs(ProtoValuationOutput.VAR)
            .addComputedOutputs(ProtoValuationOutput.GREEKS)
            .setCalculatedAt(now)
            .build()

        val result = response.toDomainValuation()

        result.positionGreeks shouldHaveSize 2

        val first = result.positionGreeks[0]
        first.instrumentId shouldBe "AAPL-OPT-JAN25-150C"
        first.delta shouldBe 0.65
        first.gamma shouldBe 0.03
        first.vega shouldBe 125.4
        first.theta shouldBe -8.2
        first.rho shouldBe 12.1

        val second = result.positionGreeks[1]
        second.instrumentId shouldBe "MSFT-OPT-FEB25-400P"
        second.delta shouldBe -0.35
        second.gamma shouldBe 0.02
        second.vega shouldBe 98.7
        second.theta shouldBe -5.6
        second.rho shouldBe -7.3
    }

    test("positionGreeks is empty list when proto response has no position_greeks") {
        val now = Timestamp.newBuilder().setSeconds(1700000000).build()
        val response = ValuationResponse.newBuilder()
            .setBookId(com.kinetix.proto.common.BookId.newBuilder().setValue("port-no-opts"))
            .setCalculationType(RiskCalculationType.PARAMETRIC)
            .setConfidenceLevel(ProtoConfidenceLevel.CL_95)
            .setVarValue(5000.0)
            .setExpectedShortfall(6500.0)
            .addComputedOutputs(ProtoValuationOutput.VAR)
            .setCalculatedAt(now)
            .build()

        val result = response.toDomainValuation()

        result.positionGreeks.shouldBeEmpty()
    }
})
