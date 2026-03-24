package com.kinetix.gateway.websocket

import com.kinetix.common.kafka.events.IntradayPnlEvent
import com.kinetix.gateway.module
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.time.Instant

private fun pnlEvent(bookId: String = "book-1") = IntradayPnlEvent(
    bookId = bookId,
    snapshotAt = Instant.parse("2026-03-24T09:30:00Z").toString(),
    baseCurrency = "USD",
    trigger = "position_change",
    totalPnl = "1500.00",
    realisedPnl = "500.00",
    unrealisedPnl = "1000.00",
    deltaPnl = "1200.00",
    gammaPnl = "80.00",
    vegaPnl = "40.00",
    thetaPnl = "-15.00",
    rhoPnl = "7.00",
    unexplainedPnl = "188.00",
    highWaterMark = "1800.00",
    correlationId = "corr-abc",
)

class PnlWebSocketTest : FunSpec({

    test("subscribe then receive P&L update for the subscribed book") {
        val broadcaster = PnlBroadcaster()

        testApplication {
            application { module(broadcaster) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/ws/pnl") {
                send(Frame.Text("""{"type":"subscribe","bookId":"book-1"}"""))
                delay(50)

                broadcaster.broadcast(pnlEvent(bookId = "book-1"))

                val frame = incoming.receive() as Frame.Text
                val json = Json.parseToJsonElement(frame.readText()).jsonObject
                json["type"]?.jsonPrimitive?.content shouldBe "pnl"
                json["bookId"]?.jsonPrimitive?.content shouldBe "book-1"
                json["totalPnl"]?.jsonPrimitive?.content shouldBe "1500.00"
                json["realisedPnl"]?.jsonPrimitive?.content shouldBe "500.00"
                json["unrealisedPnl"]?.jsonPrimitive?.content shouldBe "1000.00"
                json["highWaterMark"]?.jsonPrimitive?.content shouldBe "1800.00"
                json["trigger"]?.jsonPrimitive?.content shouldBe "position_change"
                json["correlationId"]?.jsonPrimitive?.content shouldBe "corr-abc"
            }
        }
    }

    test("subscriber does not receive updates for a different book") {
        val broadcaster = PnlBroadcaster()

        testApplication {
            application { module(broadcaster) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/ws/pnl") {
                send(Frame.Text("""{"type":"subscribe","bookId":"book-1"}"""))
                delay(50)

                // broadcast for a different book first
                broadcaster.broadcast(pnlEvent(bookId = "book-other"))
                // then for the subscribed book
                broadcaster.broadcast(pnlEvent(bookId = "book-1"))

                val frame = incoming.receive() as Frame.Text
                val json = Json.parseToJsonElement(frame.readText()).jsonObject
                json["bookId"]?.jsonPrimitive?.content shouldBe "book-1"
            }
        }
    }

    test("unknown message type returns error") {
        val broadcaster = PnlBroadcaster()

        testApplication {
            application { module(broadcaster) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/ws/pnl") {
                send(Frame.Text("""{"type":"unknown","bookId":"book-1"}"""))

                val frame = incoming.receive() as Frame.Text
                frame.readText() shouldContain "Unknown message type"
            }
        }
    }

    test("malformed JSON returns error") {
        val broadcaster = PnlBroadcaster()

        testApplication {
            application { module(broadcaster) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/ws/pnl") {
                send(Frame.Text("not json"))

                val frame = incoming.receive() as Frame.Text
                frame.readText() shouldContain "Invalid JSON"
            }
        }
    }
})
