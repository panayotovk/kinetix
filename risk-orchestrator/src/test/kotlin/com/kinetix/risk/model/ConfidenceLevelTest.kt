package com.kinetix.risk.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ConfidenceLevelTest : FunSpec({

    test("CL_975 has value 0.975 for FRTB Expected Shortfall") {
        ConfidenceLevel.CL_975.value shouldBe 0.975
    }

    test("all confidence levels are ordered by value") {
        val sorted = ConfidenceLevel.entries.sortedBy { it.value }
        sorted.map { it.name } shouldContainExactly listOf("CL_95", "CL_975", "CL_99")
    }

    test("confidence level values match their names") {
        ConfidenceLevel.CL_95.value shouldBe 0.95
        ConfidenceLevel.CL_975.value shouldBe 0.975
        ConfidenceLevel.CL_99.value shouldBe 0.99
    }
})
