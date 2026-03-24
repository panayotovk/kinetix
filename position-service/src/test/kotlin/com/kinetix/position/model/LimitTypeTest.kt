package com.kinetix.position.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class LimitTypeTest : FunSpec({

    test("LimitType includes VAR_BUDGET for soft risk budget tracking") {
        LimitType.entries.map { it.name } shouldContain "VAR_BUDGET"
    }

    test("VAR_BUDGET is distinct from VAR — VAR blocks trades, VAR_BUDGET only fires alerts") {
        val limitType = LimitType.VAR_BUDGET
        limitType.name shouldBe "VAR_BUDGET"
        (limitType == LimitType.VAR) shouldBe false
    }

    test("LimitType has all expected values including VAR_BUDGET") {
        val names = LimitType.entries.map { it.name }.toSet()
        names shouldContain "POSITION"
        names shouldContain "NOTIONAL"
        names shouldContain "VAR"
        names shouldContain "CONCENTRATION"
        names shouldContain "ADV_CONCENTRATION"
        names shouldContain "VAR_BUDGET"
    }
})
