package com.kinetix.gateway.testing

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-process Ktor HTTP server harness for gateway contract tests. Stands up an
 * embedded Netty server on a random port and lets tests register route handlers
 * via the same routing DSL the production app uses. Equivalent in spirit to the
 * gRPC fake-server pattern: the gateway's real `HttpXxxServiceClient` calls a
 * real HTTP server, so serialisation, content negotiation, and HTTP wire
 * behaviour are all exercised.
 *
 * Every received request is recorded in [recordedRequests] so tests can assert
 * what the gateway actually sent without resorting to mock interaction checks.
 */
class BackendStubServer(
    routes: Routing.() -> Unit,
) : AutoCloseable {

    data class RecordedRequest(
        val method: String,
        val path: String,
        val query: Map<String, List<String>>,
        val headers: Map<String, List<String>>,
    )

    private val recorded = ConcurrentLinkedQueue<RecordedRequest>()

    val recordedRequests: List<RecordedRequest> get() = recorded.toList()

    private val recorderPlugin = createApplicationPlugin(name = "BackendStubRecorder") {
        onCall { call ->
            recorded.add(
                RecordedRequest(
                    method = call.request.httpMethod.value,
                    path = call.request.path(),
                    query = call.request.queryParameters.entries().associate { it.key to it.value },
                    headers = call.request.headers.entries().associate { it.key to it.value },
                ),
            )
        }
    }

    private val server = embeddedServer(Netty, port = 0) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(recorderPlugin)
        routing { routes() }
    }.start(wait = false)

    val port: Int = runBlocking {
        server.engine.resolvedConnectors().first().port
    }

    val baseUrl: String = "http://localhost:$port"

    override fun close() {
        server.stop(gracePeriodMillis = 100, timeoutMillis = 1_000)
    }
}
