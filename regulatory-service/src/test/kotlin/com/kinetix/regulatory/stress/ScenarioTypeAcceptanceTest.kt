package com.kinetix.regulatory.stress

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.module
import com.kinetix.regulatory.persistence.FrtbCalculationRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID

class ScenarioTypeAcceptanceTest : FunSpec({

    val repository = mockk<StressScenarioRepository>()
    val service = StressScenarioService(repository)

    test("creates a PARAMETRIC scenario when no scenarioType specified") {
        coEvery { repository.save(any()) } returns Unit

        val result = service.create(
            name = "Parametric Test",
            description = "Default type scenario",
            shocks = "{}",
            createdBy = "analyst-1",
        )

        result.scenarioType shouldBe ScenarioType.PARAMETRIC
    }

    test("creates a HISTORICAL_REPLAY scenario when scenarioType is specified") {
        coEvery { repository.save(any()) } returns Unit

        val result = service.create(
            name = "GFC 2008 Replay",
            description = "Historical replay of GFC",
            shocks = "{}",
            createdBy = "analyst-1",
            scenarioType = ScenarioType.HISTORICAL_REPLAY,
        )

        result.scenarioType shouldBe ScenarioType.HISTORICAL_REPLAY
        coVerify { repository.save(match { it.scenarioType == ScenarioType.HISTORICAL_REPLAY }) }
    }

    test("creates a REVERSE_STRESS scenario when scenarioType is REVERSE_STRESS") {
        coEvery { repository.save(any()) } returns Unit

        val result = service.create(
            name = "Reverse Stress 100k",
            description = "Find minimum shock for 100k loss",
            shocks = "{}",
            createdBy = "analyst-1",
            scenarioType = ScenarioType.REVERSE_STRESS,
        )

        result.scenarioType shouldBe ScenarioType.REVERSE_STRESS
    }

    test("POST /api/v1/stress-scenarios with scenarioType creates scenario of correct type") {
        coEvery { repository.save(any()) } returns Unit

        testApplication {
            application {
                module(mockk<FrtbCalculationRepository>(), mockk<RiskOrchestratorClient>())
                routing { stressScenarioRoutes(service) }
            }
            val response = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{"name":"GFC Replay","description":"Historical GFC replay","shocks":"{}","createdBy":"analyst-1","scenarioType":"HISTORICAL_REPLAY"}"""
                )
            }
            response.status shouldBe HttpStatusCode.Created
            val body = response.bodyAsText()
            body shouldContain "\"scenarioType\":\"HISTORICAL_REPLAY\""
        }
    }

    test("GET /api/v1/stress-scenarios returns scenarioType in response") {
        val scenarios = listOf(
            StressScenario(
                id = UUID.randomUUID().toString(),
                name = "GFC Replay",
                description = "Historical replay",
                shocks = "{}",
                status = ScenarioStatus.DRAFT,
                createdBy = "analyst-1",
                approvedBy = null,
                approvedAt = null,
                createdAt = Instant.now(),
                scenarioType = ScenarioType.HISTORICAL_REPLAY,
            ),
        )
        coEvery { repository.findAll() } returns scenarios

        testApplication {
            application {
                module(mockk<FrtbCalculationRepository>(), mockk<RiskOrchestratorClient>())
                routing { stressScenarioRoutes(service) }
            }
            val response = client.get("/api/v1/stress-scenarios")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"scenarioType\":\"HISTORICAL_REPLAY\""
        }
    }

    test("scenarioType defaults to PARAMETRIC when not specified in request") {
        coEvery { repository.save(any()) } returns Unit

        testApplication {
            application {
                module(mockk<FrtbCalculationRepository>(), mockk<RiskOrchestratorClient>())
                routing { stressScenarioRoutes(service) }
            }
            val response = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{"name":"Default Scenario","description":"No type specified","shocks":"{}","createdBy":"analyst-1"}"""
                )
            }
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText() shouldContain "\"scenarioType\":\"PARAMETRIC\""
        }
    }
})
