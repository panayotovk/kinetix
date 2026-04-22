package com.kinetix.regulatory.persistence

import com.kinetix.regulatory.governance.ModelVersion
import com.kinetix.regulatory.governance.ModelVersionStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val NOW: Instant = Instant.parse("2025-01-15T10:00:00Z")

private fun modelVersion(
    id: String = UUID.randomUUID().toString(),
    modelName: String = "ParametricVaR",
    version: String = "1.0.0",
    status: ModelVersionStatus = ModelVersionStatus.DRAFT,
    parameters: String = """{"alpha":0.99,"windowDays":250}""",
    registeredBy: String = "quant-1",
    approvedBy: String? = null,
    approvedAt: Instant? = null,
    createdAt: Instant = NOW,
    modelTier: String? = null,
    validationReportUrl: String? = null,
    knownLimitations: String? = null,
    approvedUseCases: String? = null,
    nextValidationDate: LocalDate? = null,
) = ModelVersion(
    id = id,
    modelName = modelName,
    version = version,
    status = status,
    parameters = parameters,
    registeredBy = registeredBy,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    createdAt = createdAt,
    modelTier = modelTier,
    validationReportUrl = validationReportUrl,
    knownLimitations = knownLimitations,
    approvedUseCases = approvedUseCases,
    nextValidationDate = nextValidationDate,
)

class ExposedModelVersionRepositoryIntegrationTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository = ExposedModelVersionRepository()

    beforeEach {
        newSuspendedTransaction { ModelVersionsTable.deleteAll() }
    }

    test("save persists a DRAFT model version and findById round-trips every field") {
        val model = modelVersion(
            modelName = "HistoricalVaR",
            version = "2.1.0",
            modelTier = "TIER_1",
            validationReportUrl = "https://validation.example.com/reports/hvar-2.1.0",
            knownLimitations = "Does not model jump risk in FX pairs",
            approvedUseCases = "Daily VaR reporting, end-of-day regulatory submission",
            nextValidationDate = LocalDate.of(2026, 1, 15),
        )

        repository.save(model)

        val retrieved = repository.findById(model.id)
        retrieved.shouldNotBeNull()
        retrieved.modelName shouldBe "HistoricalVaR"
        retrieved.version shouldBe "2.1.0"
        retrieved.status shouldBe ModelVersionStatus.DRAFT
        retrieved.registeredBy shouldBe "quant-1"
        retrieved.approvedBy.shouldBeNull()
        retrieved.approvedAt.shouldBeNull()
        retrieved.modelTier shouldBe "TIER_1"
        retrieved.validationReportUrl shouldBe "https://validation.example.com/reports/hvar-2.1.0"
        retrieved.knownLimitations shouldBe "Does not model jump risk in FX pairs"
        retrieved.approvedUseCases shouldBe "Daily VaR reporting, end-of-day regulatory submission"
        retrieved.nextValidationDate shouldBe LocalDate.of(2026, 1, 15)
    }

    test("findById returns null for unknown id") {
        repository.findById("does-not-exist").shouldBeNull()
    }

    test("save overwrites an existing row — status transition from DRAFT to VALIDATED to APPROVED persists") {
        val id = UUID.randomUUID().toString()
        val draft = modelVersion(id = id, status = ModelVersionStatus.DRAFT)
        repository.save(draft)

        val validated = draft.copy(status = ModelVersionStatus.VALIDATED)
        repository.save(validated)
        repository.findById(id)!!.status shouldBe ModelVersionStatus.VALIDATED

        val approvedAt = Instant.parse("2025-02-01T12:00:00Z")
        val approved = validated.copy(
            status = ModelVersionStatus.APPROVED,
            approvedBy = "risk-officer-1",
            approvedAt = approvedAt,
        )
        repository.save(approved)

        val finalState = repository.findById(id)!!
        finalState.status shouldBe ModelVersionStatus.APPROVED
        finalState.approvedBy shouldBe "risk-officer-1"
        finalState.approvedAt shouldBe approvedAt
    }

    test("findAll returns all persisted model versions ordered by createdAt descending") {
        val oldest = modelVersion(
            modelName = "Model-A",
            createdAt = Instant.parse("2025-01-10T10:00:00Z"),
        )
        val middle = modelVersion(
            modelName = "Model-B",
            createdAt = Instant.parse("2025-01-12T10:00:00Z"),
        )
        val newest = modelVersion(
            modelName = "Model-C",
            createdAt = Instant.parse("2025-01-15T10:00:00Z"),
        )
        repository.save(oldest)
        repository.save(middle)
        repository.save(newest)

        val all = repository.findAll()
        all shouldHaveSize 3
        all.map { it.modelName } shouldBe listOf("Model-C", "Model-B", "Model-A")
    }

    test("JSONB parameters round-trip — complex nested JSON is preserved") {
        val complexParams = """{"alpha":0.99,"method":"parametric","weights":[0.25,0.25,0.5],"nested":{"enabled":true}}"""
        val model = modelVersion(parameters = complexParams)

        repository.save(model)

        val retrieved = repository.findById(model.id)!!
        // Exposed serialises JSONB back through kotlinx-serialization, so the
        // round-tripped string must parse back to an equivalent element.
        val original = kotlinx.serialization.json.Json.parseToJsonElement(complexParams)
        val roundTripped = kotlinx.serialization.json.Json.parseToJsonElement(retrieved.parameters)
        roundTripped shouldBe original
    }
})
