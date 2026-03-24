package com.kinetix.risk.routes

import com.kinetix.risk.model.HierarchyLevel
import com.kinetix.risk.model.HierarchyNodeRisk
import com.kinetix.risk.model.RiskContributor
import com.kinetix.risk.routes.dtos.HierarchyNodeRiskResponse
import com.kinetix.risk.service.HierarchyRiskService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

private fun firmNode(
    varValue: Double = 2_500_000.0,
    isPartial: Boolean = false,
    missingBooks: List<String> = emptyList(),
) = HierarchyNodeRisk(
    level = HierarchyLevel.FIRM,
    entityId = "FIRM",
    entityName = "FIRM",
    parentId = null,
    varValue = varValue,
    expectedShortfall = varValue * 1.25,
    pnlToday = null,
    limitUtilisation = null,
    marginalVar = null,
    incrementalVar = null,
    topContributors = listOf(
        RiskContributor("div-equities", "Equities", 1_500_000.0, 60.0),
        RiskContributor("div-rates", "Rates", 1_000_000.0, 40.0),
    ),
    childCount = 2,
    isPartial = isPartial,
    missingBooks = missingBooks,
)

class HierarchyRiskRoutesAcceptanceTest : FunSpec({

    val hierarchyRiskService = mockk<HierarchyRiskService>()

    beforeEach { clearMocks(hierarchyRiskService) }

    fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            install(ContentNegotiation) { json(json) }
            routing { hierarchyRiskRoutes(hierarchyRiskService) }
            block()
        }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    test("GET /api/v1/risk/hierarchy/FIRM/FIRM returns 200 with correct response shape") {
        coEvery { hierarchyRiskService.aggregateHierarchy(HierarchyLevel.FIRM, "FIRM") } returns firmNode()

        testApp {
            val response = client.get("/api/v1/risk/hierarchy/FIRM/FIRM")

            response.status shouldBe HttpStatusCode.OK

            val body = json.decodeFromString<HierarchyNodeRiskResponse>(response.bodyAsText())
            body.level shouldBe "FIRM"
            body.entityId shouldBe "FIRM"
            body.varValue shouldBe "2500000.00"
            body.isPartial shouldBe false
            body.missingBooks shouldBe emptyList()
            body.topContributors.size shouldBe 2
        }
    }

    test("GET /api/v1/risk/hierarchy/DESK/desk-rates returns 200 for desk-level node") {
        val deskNode = HierarchyNodeRisk(
            level = HierarchyLevel.DESK,
            entityId = "desk-rates",
            entityName = "Rates Desk",
            parentId = "div-rates",
            varValue = 900_000.0,
            expectedShortfall = 1_100_000.0,
            pnlToday = null,
            limitUtilisation = null,
            marginalVar = null,
            incrementalVar = null,
            topContributors = listOf(
                RiskContributor("book-r1", "Rates Book 1", 500_000.0, 55.6),
            ),
            childCount = 2,
            isPartial = false,
            missingBooks = emptyList(),
        )
        coEvery { hierarchyRiskService.aggregateHierarchy(HierarchyLevel.DESK, "desk-rates") } returns deskNode

        testApp {
            val response = client.get("/api/v1/risk/hierarchy/DESK/desk-rates")

            response.status shouldBe HttpStatusCode.OK

            val body = json.decodeFromString<HierarchyNodeRiskResponse>(response.bodyAsText())
            body.level shouldBe "DESK"
            body.entityId shouldBe "desk-rates"
            body.entityName shouldBe "Rates Desk"
            body.parentId shouldBe "div-rates"
            body.varValue shouldBe "900000.00"
            body.childCount shouldBe 2
        }
    }

    test("response includes topContributors array with contributor fields") {
        coEvery { hierarchyRiskService.aggregateHierarchy(HierarchyLevel.FIRM, "FIRM") } returns firmNode()

        testApp {
            val response = client.get("/api/v1/risk/hierarchy/FIRM/FIRM")

            val raw = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val contributors = raw["topContributors"]!!.jsonArray
            contributors.size shouldBe 2
            val first = contributors[0].jsonObject
            first["entityId"]!!.jsonPrimitive.content shouldBe "div-equities"
            first["entityName"]!!.jsonPrimitive.content shouldBe "Equities"
            first.containsKey("varContribution") shouldBe true
            first.containsKey("pctOfTotal") shouldBe true
        }
    }

    test("isPartial and missingBooks are surfaced in response when books lack VaR") {
        coEvery {
            hierarchyRiskService.aggregateHierarchy(HierarchyLevel.FIRM, "FIRM")
        } returns firmNode(isPartial = true, missingBooks = listOf("book-missing-1", "book-missing-2"))

        testApp {
            val response = client.get("/api/v1/risk/hierarchy/FIRM/FIRM")

            response.status shouldBe HttpStatusCode.OK

            val body = json.decodeFromString<HierarchyNodeRiskResponse>(response.bodyAsText())
            body.isPartial shouldBe true
            body.missingBooks shouldBe listOf("book-missing-1", "book-missing-2")
        }
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    test("GET /api/v1/risk/hierarchy/FIRM/FIRM returns 404 when entity cannot be resolved") {
        coEvery { hierarchyRiskService.aggregateHierarchy(HierarchyLevel.FIRM, "FIRM") } returns null

        testApp {
            val response = client.get("/api/v1/risk/hierarchy/FIRM/FIRM")

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /api/v1/risk/hierarchy/INVALID/x returns 400 for unknown hierarchy level") {
        testApp {
            val response = client.get("/api/v1/risk/hierarchy/INVALID/x")

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
