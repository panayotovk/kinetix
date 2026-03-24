package com.kinetix.gateway.websocket

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

fun Route.pnlWebSocket(broadcaster: PnlBroadcaster) {
    webSocket("/ws/pnl") {
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val message = try {
                        Json.decodeFromString<PnlSubscribeMessage>(text)
                    } catch (_: Exception) {
                        send(Frame.Text("""{"error":"Invalid JSON"}"""))
                        continue
                    }
                    when (message.type) {
                        "subscribe" -> broadcaster.subscribe(this, message.bookId)
                        else -> send(Frame.Text("""{"error":"Unknown message type: ${message.type}"}"""))
                    }
                }
            }
        } finally {
            broadcaster.removeSession(this)
        }
    }
}
