package com.kinetix.regulatory.stress

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID

class StressScenarioUpdateTest : FunSpec({

    val repository = mockk<StressScenarioRepository>()
    val service = StressScenarioService(repository)

    test("updating a draft scenario increments version and keeps status as draft") {
        val id = UUID.randomUUID().toString()
        val scenario = aScenario(id = id, status = ScenarioStatus.DRAFT, version = 1)
        coEvery { repository.findById(id) } returns scenario
        coEvery { repository.save(any()) } returns Unit

        val result = service.update(id, shocks = """{"equity":-0.30}""")

        result.version shouldBe 2
        result.status shouldBe ScenarioStatus.DRAFT
        result.shocks shouldBe """{"equity":-0.30}"""
        coVerify(exactly = 1) { repository.save(any()) }
    }

    test("updating an approved scenario resets status to draft, clears approvedBy and approvedAt, and increments version") {
        val id = UUID.randomUUID().toString()
        val scenario = aScenario(
            id = id,
            status = ScenarioStatus.APPROVED,
            version = 3,
            approvedBy = "risk-manager-1",
            approvedAt = Instant.now(),
        )
        coEvery { repository.findById(id) } returns scenario
        coEvery { repository.save(any()) } returns Unit

        val result = service.update(id, shocks = """{"equity":-0.50}""")

        result.version shouldBe 4
        result.status shouldBe ScenarioStatus.DRAFT
        result.approvedBy shouldBe null
        result.approvedAt shouldBe null
        result.shocks shouldBe """{"equity":-0.50}"""
    }

    test("updating a pending_approval scenario resets status to draft and increments version") {
        val id = UUID.randomUUID().toString()
        val scenario = aScenario(id = id, status = ScenarioStatus.PENDING_APPROVAL, version = 2)
        coEvery { repository.findById(id) } returns scenario
        coEvery { repository.save(any()) } returns Unit

        val result = service.update(id, shocks = """{"ir":0.02}""")

        result.version shouldBe 3
        result.status shouldBe ScenarioStatus.DRAFT
        result.approvedBy shouldBe null
        result.approvedAt shouldBe null
    }

    test("updating a retired scenario throws IllegalStateException") {
        val id = UUID.randomUUID().toString()
        val scenario = aScenario(id = id, status = ScenarioStatus.RETIRED)
        coEvery { repository.findById(id) } returns scenario

        shouldThrow<IllegalStateException> {
            service.update(id, shocks = """{"equity":-0.10}""")
        }
    }

    test("update only changes specified fields, leaving others unchanged") {
        val id = UUID.randomUUID().toString()
        val scenario = aScenario(
            id = id,
            status = ScenarioStatus.DRAFT,
            version = 1,
            shocks = """{"equity":-0.20}""",
            correlationOverride = """{"equity_ir":0.3}""",
            liquidityStressFactors = """{"bid_ask":1.5}""",
        )
        coEvery { repository.findById(id) } returns scenario
        coEvery { repository.save(any()) } returns Unit

        val result = service.update(id, shocks = """{"equity":-0.40}""")

        result.shocks shouldBe """{"equity":-0.40}"""
        result.correlationOverride shouldBe """{"equity_ir":0.3}"""
        result.liquidityStressFactors shouldBe """{"bid_ask":1.5}"""
        result.version shouldBe 2
    }
})

private fun aScenario(
    id: String = UUID.randomUUID().toString(),
    name: String = "Test Scenario",
    description: String = "Test description",
    shocks: String = """{"equity":-0.20}""",
    status: ScenarioStatus = ScenarioStatus.DRAFT,
    createdBy: String = "risk-analyst-1",
    approvedBy: String? = null,
    approvedAt: Instant? = null,
    createdAt: Instant = Instant.now(),
    version: Int = 1,
    correlationOverride: String? = null,
    liquidityStressFactors: String? = null,
) = StressScenario(
    id = id,
    name = name,
    description = description,
    shocks = shocks,
    status = status,
    createdBy = createdBy,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    createdAt = createdAt,
    version = version,
    correlationOverride = correlationOverride,
    liquidityStressFactors = liquidityStressFactors,
)
