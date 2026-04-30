package com.kinetix.risk.grpc

import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import java.util.concurrent.TimeUnit

/**
 * Generic fake-server harness for acceptance tests that need to stand in for a real
 * gRPC service. Binds the supplied [BindableService] implementations to an in-JVM
 * Netty gRPC server on a random localhost port so calls travel over real HTTP/2 —
 * interceptors, serialisation, and channel wiring all exercised.
 *
 * Use like:
 * ```
 * GrpcFakeServer(FakeRiskCalculationService(...)).use { fake ->
 *     val stub = RiskCalculationServiceCoroutineStub(fake.channel())
 *     // ...
 * }
 * ```
 */
class GrpcFakeServer(vararg services: BindableService) : AutoCloseable {

    private val server: Server = NettyServerBuilder.forPort(0)
        .also { builder -> services.forEach(builder::addService) }
        .build()
        .start()

    val port: Int get() = server.port

    fun channel(): ManagedChannel =
        ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .build()

    override fun close() {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }
}
