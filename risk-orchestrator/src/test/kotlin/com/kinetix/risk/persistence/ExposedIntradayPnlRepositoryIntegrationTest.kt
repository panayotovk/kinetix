package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.IntradayPnlSnapshot
import com.kinetix.risk.model.PnlTrigger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.Instant

private val BOOK = BookId("book-1")
private val BOOK2 = BookId("book-2")
private fun bd(v: String) = BigDecimal(v)

private fun snapshot(
    bookId: BookId = BOOK,
    snapshotAt: Instant = Instant.parse("2026-03-24T10:00:00Z"),
    totalPnl: String = "1000.00",
    realisedPnl: String = "400.00",
    unrealisedPnl: String = "600.00",
    highWaterMark: String = "1000.00",
    trigger: PnlTrigger = PnlTrigger.POSITION_CHANGE,
) = IntradayPnlSnapshot(
    bookId = bookId,
    snapshotAt = snapshotAt,
    baseCurrency = "USD",
    trigger = trigger,
    totalPnl = bd(totalPnl),
    realisedPnl = bd(realisedPnl),
    unrealisedPnl = bd(unrealisedPnl),
    deltaPnl = bd("800.00"),
    gammaPnl = bd("50.00"),
    vegaPnl = bd("20.00"),
    thetaPnl = bd("-10.00"),
    rhoPnl = bd("5.00"),
    unexplainedPnl = bd("135.00"),
    highWaterMark = bd(highWaterMark),
    correlationId = "corr-test",
)

class ExposedIntradayPnlRepositoryIntegrationTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository: IntradayPnlRepository = ExposedIntradayPnlRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { IntradayPnlSnapshotsTable.deleteAll() }
    }

    test("saves and retrieves the latest snapshot for a book") {
        repository.save(snapshot(snapshotAt = Instant.parse("2026-03-24T10:00:00Z"), totalPnl = "900.00"))
        repository.save(snapshot(snapshotAt = Instant.parse("2026-03-24T10:01:00Z"), totalPnl = "1000.00"))

        val found = repository.findLatest(BOOK)
        found.shouldNotBeNull()
        found.bookId shouldBe BOOK
        found.totalPnl.compareTo(bd("1000.00")) shouldBe 0
    }

    test("returns null for unknown book") {
        repository.findLatest(BookId("unknown")).shouldBeNull()
    }

    test("findSeries returns snapshots within time window ordered by time ascending") {
        val t1 = Instant.parse("2026-03-24T09:00:00Z")
        val t2 = Instant.parse("2026-03-24T10:00:00Z")
        val t3 = Instant.parse("2026-03-24T11:00:00Z")
        repository.save(snapshot(snapshotAt = t1, totalPnl = "500.00"))
        repository.save(snapshot(snapshotAt = t2, totalPnl = "1000.00"))
        repository.save(snapshot(snapshotAt = t3, totalPnl = "1500.00"))

        val series = repository.findSeries(
            bookId = BOOK,
            from = Instant.parse("2026-03-24T09:30:00Z"),
            to = Instant.parse("2026-03-24T10:30:00Z"),
        )
        series shouldHaveSize 1
        series[0].totalPnl.compareTo(bd("1000.00")) shouldBe 0
    }

    test("snapshots for different books are isolated") {
        repository.save(snapshot(bookId = BOOK, totalPnl = "1000.00"))
        repository.save(snapshot(bookId = BOOK2, totalPnl = "2000.00"))

        repository.findLatest(BOOK)!!.totalPnl.compareTo(bd("1000.00")) shouldBe 0
        repository.findLatest(BOOK2)!!.totalPnl.compareTo(bd("2000.00")) shouldBe 0
    }

    test("high water mark and attribution fields are persisted correctly") {
        repository.save(snapshot(totalPnl = "1200.00", highWaterMark = "1500.00"))

        val found = repository.findLatest(BOOK)!!
        found.highWaterMark.compareTo(bd("1500.00")) shouldBe 0
        found.deltaPnl.compareTo(bd("800.00")) shouldBe 0
        found.gammaPnl.compareTo(bd("50.00")) shouldBe 0
        found.vegaPnl.compareTo(bd("20.00")) shouldBe 0
        found.thetaPnl.compareTo(bd("-10.00")) shouldBe 0
        found.rhoPnl.compareTo(bd("5.00")) shouldBe 0
        found.unexplainedPnl.compareTo(bd("135.00")) shouldBe 0
    }

    test("trigger and correlation id are persisted") {
        repository.save(snapshot(trigger = PnlTrigger.TRADE_BOOKED))

        val found = repository.findLatest(BOOK)!!
        found.trigger shouldBe PnlTrigger.TRADE_BOOKED
        found.correlationId shouldBe "corr-test"
    }

    test("findSeries with wide window returns all snapshots in ascending order") {
        val times = listOf(
            Instant.parse("2026-03-24T09:00:00Z"),
            Instant.parse("2026-03-24T10:00:00Z"),
            Instant.parse("2026-03-24T11:00:00Z"),
        )
        times.forEachIndexed { i, t ->
            repository.save(snapshot(snapshotAt = t, totalPnl = "${(i + 1) * 100}.00"))
        }

        val series = repository.findSeries(
            bookId = BOOK,
            from = Instant.parse("2026-03-24T08:00:00Z"),
            to = Instant.parse("2026-03-24T12:00:00Z"),
        )
        series shouldHaveSize 3
        series[0].totalPnl.compareTo(bd("100.00")) shouldBe 0
        series[1].totalPnl.compareTo(bd("200.00")) shouldBe 0
        series[2].totalPnl.compareTo(bd("300.00")) shouldBe 0
    }
})
