package com.kinetix.regulatory.persistence

import com.kinetix.regulatory.submission.RegulatorySubmission
import com.kinetix.regulatory.submission.SubmissionStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

private val NOW: Instant = Instant.parse("2025-01-15T10:00:00Z")

private fun submission(
    id: String = UUID.randomUUID().toString(),
    reportType: String = "FRTB_SA",
    status: SubmissionStatus = SubmissionStatus.DRAFT,
    preparerId: String = "preparer-1",
    approverId: String? = null,
    deadline: Instant = Instant.parse("2025-02-01T17:00:00Z"),
    submittedAt: Instant? = null,
    acknowledgedAt: Instant? = null,
    createdAt: Instant = NOW,
) = RegulatorySubmission(
    id = id,
    reportType = reportType,
    status = status,
    preparerId = preparerId,
    approverId = approverId,
    deadline = deadline,
    submittedAt = submittedAt,
    acknowledgedAt = acknowledgedAt,
    createdAt = createdAt,
)

class ExposedSubmissionRepositoryIntegrationTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository = ExposedSubmissionRepository()

    beforeEach {
        newSuspendedTransaction { SubmissionsTable.deleteAll() }
    }

    test("save persists a DRAFT submission and findById round-trips every field") {
        val s = submission(
            reportType = "FRTB_IMA",
            preparerId = "preparer-alice",
            deadline = Instant.parse("2025-03-31T23:59:59Z"),
        )

        repository.save(s)

        val retrieved = repository.findById(s.id)
        retrieved.shouldNotBeNull()
        retrieved.reportType shouldBe "FRTB_IMA"
        retrieved.status shouldBe SubmissionStatus.DRAFT
        retrieved.preparerId shouldBe "preparer-alice"
        retrieved.approverId.shouldBeNull()
        retrieved.deadline shouldBe Instant.parse("2025-03-31T23:59:59Z")
        retrieved.submittedAt.shouldBeNull()
        retrieved.acknowledgedAt.shouldBeNull()
    }

    test("findById returns null for an unknown id") {
        repository.findById("nonexistent").shouldBeNull()
    }

    test("save persists preparer and approver as distinct user ids") {
        val s = submission(
            preparerId = "preparer-alice",
            approverId = "approver-bob",
            status = SubmissionStatus.APPROVED,
        )

        repository.save(s)

        val retrieved = repository.findById(s.id)!!
        retrieved.preparerId shouldBe "preparer-alice"
        retrieved.approverId shouldBe "approver-bob"
        retrieved.status shouldBe SubmissionStatus.APPROVED
    }

    test("save overwrites existing row on status transition DRAFT -> PENDING_REVIEW -> APPROVED -> SUBMITTED") {
        val id = UUID.randomUUID().toString()
        val draft = submission(id = id, status = SubmissionStatus.DRAFT, preparerId = "preparer-1")
        repository.save(draft)

        val pending = draft.copy(status = SubmissionStatus.PENDING_REVIEW)
        repository.save(pending)
        repository.findById(id)!!.status shouldBe SubmissionStatus.PENDING_REVIEW

        val approved = pending.copy(
            status = SubmissionStatus.APPROVED,
            approverId = "approver-2",
        )
        repository.save(approved)
        repository.findById(id)!!.let {
            it.status shouldBe SubmissionStatus.APPROVED
            it.approverId shouldBe "approver-2"
        }

        val submittedAt = Instant.parse("2025-02-01T12:00:00Z")
        val submitted = approved.copy(
            status = SubmissionStatus.SUBMITTED,
            submittedAt = submittedAt,
        )
        repository.save(submitted)

        val finalState = repository.findById(id)!!
        finalState.status shouldBe SubmissionStatus.SUBMITTED
        finalState.submittedAt shouldBe submittedAt
        finalState.preparerId shouldBe "preparer-1"
        finalState.approverId shouldBe "approver-2"
    }

    test("findAll returns all submissions ordered by createdAt descending") {
        repository.save(submission(reportType = "A", createdAt = Instant.parse("2025-01-10T10:00:00Z")))
        repository.save(submission(reportType = "B", createdAt = Instant.parse("2025-01-12T10:00:00Z")))
        repository.save(submission(reportType = "C", createdAt = Instant.parse("2025-01-15T10:00:00Z")))

        val all = repository.findAll()
        all shouldHaveSize 3
        all.map { it.reportType } shouldBe listOf("C", "B", "A")
    }
})
