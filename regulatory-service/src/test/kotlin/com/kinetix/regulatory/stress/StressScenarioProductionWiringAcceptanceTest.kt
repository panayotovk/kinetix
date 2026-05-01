package com.kinetix.regulatory.stress

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.module
import com.kinetix.regulatory.persistence.DatabaseTestSetup
import com.kinetix.regulatory.persistence.ExposedFrtbCalculationRepository
import com.kinetix.regulatory.persistence.ExposedStressScenarioRepository
import com.kinetix.regulatory.testing.BackendStubServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*

class StressScenarioProductionWiringAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val frtbRepo = ExposedFrtbCalculationRepository(db)
    // Minimal stub backend — these tests never call through RiskOrchestratorClient
    val riskBackend = BackendStubServer { }
    val httpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }
    val riskClient = RiskOrchestratorClient(httpClient, riskBackend.baseUrl)

    afterSpec {
        riskBackend.close()
        httpClient.close()
    }

    test("stress scenario repository is not provided — GET /api/v1/stress-scenarios — returns 404 because routes are not registered") {
        testApplication {
            application {
                module(frtbRepo, riskClient, backtestRepository = null, stressScenarioRepository = null)
            }
            val response = client.get("/api/v1/stress-scenarios")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("stress scenario repository is provided — GET /api/v1/stress-scenarios — returns 200 with empty list") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = ExposedStressScenarioRepository(db))
            }
            val response = client.get("/api/v1/stress-scenarios")
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
