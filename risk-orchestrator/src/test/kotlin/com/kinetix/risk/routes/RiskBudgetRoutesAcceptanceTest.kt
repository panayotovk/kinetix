package com.kinetix.risk.routes

import com.kinetix.risk.model.BudgetPeriod
import com.kinetix.risk.model.HierarchyLevel
import com.kinetix.risk.model.RiskBudgetAllocation
import com.kinetix.risk.persistence.DatabaseTestSetup
import com.kinetix.risk.persistence.ExposedRiskBudgetAllocationRepository
import com.kinetix.risk.persistence.RiskBudgetAllocationsTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }

private const val ALLOC_ID_1 = "00000000-0000-0000-0000-000000000001"
private const val ALLOC_ID_2 = "00000000-0000-0000-0000-000000000002"

private fun sampleAllocation(id: String = ALLOC_ID_1) = RiskBudgetAllocation(
    id = id,
    entityLevel = HierarchyLevel.DESK,
    entityId = "desk-rates",
    budgetType = "VAR_BUDGET",
    budgetPeriod = BudgetPeriod.DAILY,
    budgetAmount = BigDecimal("5000000.00"),
    effectiveFrom = LocalDate.of(2026, 1, 1),
    effectiveTo = null,
    allocatedBy = "cro@firm.com",
    allocationNote = "Annual risk budget for rates desk",
)

class RiskBudgetRoutesAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val budgetRepository = ExposedRiskBudgetAllocationRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { RiskBudgetAllocationsTable.deleteAll() }
    }

    fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            install(ContentNegotiation) { json(json) }
            routing { riskBudgetRoutes(budgetRepository) }
            block()
        }
    }

    // ── GET /api/v1/risk/budgets ───────────────────────────────────────────────

    test("GET /api/v1/risk/budgets returns all allocations") {
        budgetRepository.save(sampleAllocation(ALLOC_ID_1))
        budgetRepository.save(sampleAllocation(ALLOC_ID_2))

        testApp {
            val response = client.get("/api/v1/risk/budgets")

            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 2
            body[0].jsonObject["id"]!!.jsonPrimitive.content shouldBe ALLOC_ID_1
        }
    }

    test("GET /api/v1/risk/budgets?level=DESK returns desk-level allocations") {
        budgetRepository.save(sampleAllocation())

        testApp {
            val response = client.get("/api/v1/risk/budgets?level=DESK")

            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 1
            body[0].jsonObject["entityLevel"]!!.jsonPrimitive.content shouldBe "DESK"
        }
    }

    // ── GET /api/v1/risk/budgets/{id} ─────────────────────────────────────────

    test("GET /api/v1/risk/budgets/{id} returns allocation when found") {
        budgetRepository.save(sampleAllocation(ALLOC_ID_1))

        testApp {
            val response = client.get("/api/v1/risk/budgets/$ALLOC_ID_1")

            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]!!.jsonPrimitive.content shouldBe ALLOC_ID_1
            body["entityId"]!!.jsonPrimitive.content shouldBe "desk-rates"
            body["budgetType"]!!.jsonPrimitive.content shouldBe "VAR_BUDGET"
            body["budgetAmount"]!!.jsonPrimitive.content shouldBe "5000000.000000"
        }
    }

    test("GET /api/v1/risk/budgets/{id} returns 404 when not found") {
        testApp {
            val response = client.get("/api/v1/risk/budgets/00000000-0000-0000-0000-000000000099")

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    // ── POST /api/v1/risk/budgets ──────────────────────────────────────────────

    test("POST /api/v1/risk/budgets creates allocation and returns 201") {
        testApp {
            val response = client.post("/api/v1/risk/budgets") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "entityLevel":"DESK","entityId":"desk-rates",
                      "budgetType":"VAR_BUDGET","budgetPeriod":"DAILY",
                      "budgetAmount":"5000000.00","effectiveFrom":"2026-01-01",
                      "allocatedBy":"cro@firm.com"
                    }
                    """.trimIndent()
                )
            }

            response.status shouldBe HttpStatusCode.Created
            val saved = budgetRepository.findAll(null, null)
            saved.size shouldBe 1
            saved[0].entityId shouldBe "desk-rates"
        }
    }

    test("POST /api/v1/risk/budgets returns 400 when budgetAmount is missing") {
        testApp {
            val response = client.post("/api/v1/risk/budgets") {
                contentType(ContentType.Application.Json)
                setBody("""{"entityLevel":"DESK","entityId":"desk-rates","budgetType":"VAR_BUDGET"}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    // ── PUT /api/v1/risk/budgets/{id} ─────────────────────────────────────────

    test("PUT updates budget amount and returns updated budget") {
        budgetRepository.save(sampleAllocation(ALLOC_ID_1))

        testApp {
            val response = client.put("/api/v1/risk/budgets/$ALLOC_ID_1") {
                contentType(ContentType.Application.Json)
                setBody("""{"budgetAmount":"7500000.00","allocationNote":"Revised budget"}""")
            }

            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]!!.jsonPrimitive.content shouldBe ALLOC_ID_1
            // Response reflects the request value before DB round-trip (scale 2)
            body["budgetAmount"]!!.jsonPrimitive.content shouldBe "7500000.00"
            body["allocationNote"]!!.jsonPrimitive.content shouldBe "Revised budget"
            // DB stores with scale 6 per decimal(24,6) column
            budgetRepository.findById(ALLOC_ID_1)!!.budgetAmount shouldBe BigDecimal("7500000.000000")
        }
    }

    test("PUT returns 404 for non-existent budget") {
        testApp {
            val response = client.put("/api/v1/risk/budgets/00000000-0000-0000-0000-000000000099") {
                contentType(ContentType.Application.Json)
                setBody("""{"budgetAmount":"1000000.00"}""")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    // ── DELETE /api/v1/risk/budgets/{id} ──────────────────────────────────────

    test("DELETE /api/v1/risk/budgets/{id} removes allocation and returns 204") {
        budgetRepository.save(sampleAllocation(ALLOC_ID_1))

        testApp {
            val response = client.delete("/api/v1/risk/budgets/$ALLOC_ID_1")

            response.status shouldBe HttpStatusCode.NoContent
            budgetRepository.findById(ALLOC_ID_1) shouldBe null
        }
    }

    test("DELETE /api/v1/risk/budgets/{id} returns 404 when not found") {
        testApp {
            val response = client.delete("/api/v1/risk/budgets/00000000-0000-0000-0000-000000000099")

            response.status shouldBe HttpStatusCode.NotFound
        }
    }
})
