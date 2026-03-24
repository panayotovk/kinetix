package com.kinetix.gateway.websocket

import com.kinetix.common.kafka.events.IntradayPnlEvent
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class PnlBroadcaster {

    private val json = Json { encodeDefaults = true }
    private val subscriptions = ConcurrentHashMap<String, MutableSet<WebSocketServerSession>>()

    fun subscribe(session: WebSocketServerSession, bookId: String) {
        subscriptions.computeIfAbsent(bookId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun removeSession(session: WebSocketServerSession) {
        for (sessions in subscriptions.values) {
            sessions.remove(session)
        }
    }

    suspend fun broadcast(event: IntradayPnlEvent) {
        val sessions = subscriptions[event.bookId] ?: return
        val message = json.encodeToString(PnlUpdate.from(event))
        val dead = mutableListOf<WebSocketServerSession>()
        for (session in sessions) {
            try {
                session.send(Frame.Text(message))
            } catch (_: Exception) {
                dead.add(session)
            }
        }
        for (session in dead) {
            removeSession(session)
        }
    }
}
