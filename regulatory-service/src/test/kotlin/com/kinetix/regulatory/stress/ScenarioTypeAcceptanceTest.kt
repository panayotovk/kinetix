package com.kinetix.regulatory.stress

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.module
import com.kinetix.regulatory.persistence.DatabaseTestSetup
import com.kinetix.regulatory.persistence.ExposedFrtbCalculationRepository
import com.kinetix.regulatory.persistence.ExposedStressScenarioRepository
import com.kinetix.regulatory.persistence.StressScenariosTable
import com.kinetix.regulatory.testing.BackendStubServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ScenarioTypeAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository = ExposedStressScenarioRepository(db)
    val frtbRepo = ExposedFrtbCalculationRepository(db)
    val service = StressScenarioService(repository)

    // Minimal stub backend — these tests never exercise RiskOrchestratorClient
    val riskBackend = BackendStubServer { }
    val httpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }
    val riskClient = RiskOrchestratorClient(httpClient, riskBackend.baseUrl)

    beforeEach {
        newSuspendedTransaction(db = db) { StressScenariosTable.deleteAll() }
    }

    afterSpec {
        riskBackend.close()
        httpClient.close()
    }

    test("creates a PARAMETRIC scenario when no scenarioType specified") {
        val result = service.create(
            name = "Parametric Test",
            description = "Default type scenario",
            shocks = "{}",
            createdBy = "analyst-1",
        )

        result.scenarioType shouldBe ScenarioType.PARAMETRIC
        repository.findById(result.id)?.scenarioType shouldBe ScenarioType.PARAMETRIC
    }

    test("creates a HISTORICAL_REPLAY scenario when scenarioType is specified") {
        val result = service.create(
            name = "GFC 2008 Replay",
            description = "Historical replay of GFC",
            shocks = "{}",
            createdBy = "analyst-1",
            scenarioType = ScenarioType.HISTORICAL_REPLAY,
        )

        result.scenarioType shouldBe ScenarioType.HISTORICAL_REPLAY
        repository.findById(result.id)?.scenarioType shouldBe ScenarioType.HISTORICAL_REPLAY
    }

    test("creates a REVERSE_STRESS scenario when scenarioType is REVERSE_STRESS") {
        val result = service.create(
            name = "Reverse Stress 100k",
            description = "Find minimum shock for 100k loss",
            shocks = "{}",
            createdBy = "analyst-1",
            scenarioType = ScenarioType.REVERSE_STRESS,
        )

        result.scenarioType shouldBe ScenarioType.REVERSE_STRESS
        repository.findById(result.id)?.scenarioType shouldBe ScenarioType.REVERSE_STRESS
    }

    test("POST /api/v1/stress-scenarios with scenarioType creates scenario of correct type") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = repository)
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

        val saved = repository.findAll().firstOrNull { it.name == "GFC Replay" }
        saved?.scenarioType shouldBe ScenarioType.HISTORICAL_REPLAY
    }

    test("GET /api/v1/stress-scenarios returns scenarioType in response") {
        repository.save(
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

        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = repository)
                routing { stressScenarioRoutes(service) }
            }
            val response = client.get("/api/v1/stress-scenarios")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"scenarioType\":\"HISTORICAL_REPLAY\""
        }
    }

    test("scenarioType defaults to PARAMETRIC when not specified in request") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = repository)
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

        val saved = repository.findAll().firstOrNull { it.name == "Default Scenario" }
        saved?.scenarioType shouldBe ScenarioType.PARAMETRIC
    }
})
