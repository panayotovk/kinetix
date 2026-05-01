package com.kinetix.referencedata.service

import com.kinetix.common.model.LiquidityTier
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InstrumentLiquidityServiceTest : FunSpec({

    context("classifyTier") {
        test("HIGH_LIQUID when adv is above 50 million and spread is below 5 bps") {
            InstrumentLiquidityService.classifyTier(adv = 75_000_000.0, bidAskSpreadBps = 3.0) shouldBe LiquidityTier.HIGH_LIQUID
        }

        test("HIGH_LIQUID boundary: adv exactly 50 million and spread exactly 5 bps") {
            InstrumentLiquidityService.classifyTier(adv = 50_000_000.0, bidAskSpreadBps = 5.0) shouldBe LiquidityTier.HIGH_LIQUID
        }

        test("not HIGH_LIQUID when adv is above 50 million but spread is above 5 bps") {
            InstrumentLiquidityService.classifyTier(adv = 75_000_000.0, bidAskSpreadBps = 6.0) shouldBe LiquidityTier.LIQUID
        }

        test("not HIGH_LIQUID when spread is below 5 bps but adv is below 50 million") {
            InstrumentLiquidityService.classifyTier(adv = 49_000_000.0, bidAskSpreadBps = 2.0) shouldBe LiquidityTier.LIQUID
        }

        test("LIQUID when adv is above 10 million and spread is below 20 bps") {
            InstrumentLiquidityService.classifyTier(adv = 25_000_000.0, bidAskSpreadBps = 10.0) shouldBe LiquidityTier.LIQUID
        }

        test("LIQUID boundary: adv exactly 10 million and spread exactly 20 bps") {
            InstrumentLiquidityService.classifyTier(adv = 10_000_000.0, bidAskSpreadBps = 20.0) shouldBe LiquidityTier.LIQUID
        }

        test("not LIQUID when adv is above 10 million but spread is above 20 bps") {
            InstrumentLiquidityService.classifyTier(adv = 25_000_000.0, bidAskSpreadBps = 21.0) shouldBe LiquidityTier.SEMI_LIQUID
        }

        test("SEMI_LIQUID when adv is above 1 million regardless of spread") {
            InstrumentLiquidityService.classifyTier(adv = 5_000_000.0, bidAskSpreadBps = 50.0) shouldBe LiquidityTier.SEMI_LIQUID
        }

        test("SEMI_LIQUID boundary: adv exactly 1 million") {
            InstrumentLiquidityService.classifyTier(adv = 1_000_000.0, bidAskSpreadBps = 100.0) shouldBe LiquidityTier.SEMI_LIQUID
        }

        test("ILLIQUID when adv is below 1 million") {
            InstrumentLiquidityService.classifyTier(adv = 500_000.0, bidAskSpreadBps = 5.0) shouldBe LiquidityTier.ILLIQUID
        }

        test("ILLIQUID when adv is zero") {
            InstrumentLiquidityService.classifyTier(adv = 0.0, bidAskSpreadBps = 0.0) shouldBe LiquidityTier.ILLIQUID
        }
    }
})
