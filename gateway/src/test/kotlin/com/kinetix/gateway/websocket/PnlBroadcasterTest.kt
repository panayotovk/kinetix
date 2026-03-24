package com.kinetix.gateway.websocket

import com.kinetix.common.kafka.events.IntradayPnlEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.mockk.*
import java.time.Instant

private fun pnlEvent(bookId: String = "book-1", totalPnl: String = "1000.00") = IntradayPnlEvent(
    bookId = bookId,
    snapshotAt = Instant.parse("2026-03-24T09:30:00Z").toString(),
    baseCurrency = "USD",
    trigger = "position_change",
    totalPnl = totalPnl,
    realisedPnl = "400.00",
    unrealisedPnl = "600.00",
    deltaPnl = "800.00",
    gammaPnl = "50.00",
    vegaPnl = "30.00",
    thetaPnl = "-10.00",
    rhoPnl = "5.00",
    unexplainedPnl = "125.00",
    highWaterMark = "1200.00",
    correlationId = null,
)

private fun mockSession(): WebSocketServerSession {
    val session = mockk<WebSocketServerSession>(relaxed = true)
    coEvery { session.send(any<Frame>()) } just Runs
    return session
}

class PnlBroadcasterTest : FunSpec({

    test("broadcast sends to session subscribed to that book") {
        val broadcaster = PnlBroadcaster()
        val session = mockSession()
        broadcaster.subscribe(session, "book-1")

        broadcaster.broadcast(pnlEvent(bookId = "book-1"))

        coVerify(exactly = 1) { session.send(any<Frame>()) }
    }

    test("broadcast does not send to session subscribed to a different book") {
        val broadcaster = PnlBroadcaster()
        val session = mockSession()
        broadcaster.subscribe(session, "book-2")

        broadcaster.broadcast(pnlEvent(bookId = "book-1"))

        coVerify(exactly = 0) { session.send(any<Frame>()) }
    }

    test("broadcast sends to multiple sessions subscribed to the same book") {
        val broadcaster = PnlBroadcaster()
        val session1 = mockSession()
        val session2 = mockSession()
        broadcaster.subscribe(session1, "book-1")
        broadcaster.subscribe(session2, "book-1")

        broadcaster.broadcast(pnlEvent(bookId = "book-1"))

        coVerify(exactly = 1) { session1.send(any<Frame>()) }
        coVerify(exactly = 1) { session2.send(any<Frame>()) }
    }

    test("removeSession stops delivery") {
        val broadcaster = PnlBroadcaster()
        val session = mockSession()
        broadcaster.subscribe(session, "book-1")
        broadcaster.removeSession(session)

        broadcaster.broadcast(pnlEvent(bookId = "book-1"))

        coVerify(exactly = 0) { session.send(any<Frame>()) }
    }

    test("broadcast with no subscriptions is a no-op") {
        val broadcaster = PnlBroadcaster()
        // should not throw
        broadcaster.broadcast(pnlEvent())
    }

    test("dead session is auto-removed on broadcast") {
        val broadcaster = PnlBroadcaster()
        val deadSession = mockSession()
        coEvery { deadSession.send(any<Frame>()) } throws RuntimeException("Connection closed")
        broadcaster.subscribe(deadSession, "book-1")

        broadcaster.broadcast(pnlEvent(bookId = "book-1"))

        // second broadcast should not attempt send again
        broadcaster.broadcast(pnlEvent(bookId = "book-1"))
        coVerify(exactly = 1) { deadSession.send(any<Frame>()) }
    }

    test("multiple books: each subscriber receives only their book's events") {
        val broadcaster = PnlBroadcaster()
        val sessionA = mockSession()
        val sessionB = mockSession()
        broadcaster.subscribe(sessionA, "book-a")
        broadcaster.subscribe(sessionB, "book-b")

        broadcaster.broadcast(pnlEvent(bookId = "book-a"))
        broadcaster.broadcast(pnlEvent(bookId = "book-b"))
        broadcaster.broadcast(pnlEvent(bookId = "book-b"))

        coVerify(exactly = 1) { sessionA.send(any<Frame>()) }
        coVerify(exactly = 2) { sessionB.send(any<Frame>()) }
    }
})
