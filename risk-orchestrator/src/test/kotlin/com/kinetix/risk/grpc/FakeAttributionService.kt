package com.kinetix.risk.grpc

import com.kinetix.proto.risk.AttributionServiceGrpcKt
import com.kinetix.proto.risk.BrinsonAttributionRequest
import com.kinetix.proto.risk.BrinsonAttributionResponse
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test double for the gRPC `AttributionService`. Configure the response by supplying a
 * handler lambda; received requests are exposed through [calculateBrinsonAttributionRequests]
 * so tests can assert on what the orchestrator actually sent.
 *
 * Bind to a real gRPC server via [GrpcFakeServer] so calls travel over real
 * HTTP/2 — interceptors, marshalling, and channel wiring are all exercised.
 */
class FakeAttributionService(
    var calculateBrinsonAttributionHandler: (BrinsonAttributionRequest) -> BrinsonAttributionResponse =
        { BrinsonAttributionResponse.getDefaultInstance() },
) : AttributionServiceGrpcKt.AttributionServiceCoroutineImplBase() {

    val calculateBrinsonAttributionRequests: MutableList<BrinsonAttributionRequest> = CopyOnWriteArrayList()

    override suspend fun calculateBrinsonAttribution(request: BrinsonAttributionRequest): BrinsonAttributionResponse {
        calculateBrinsonAttributionRequests += request
        return calculateBrinsonAttributionHandler(request)
    }
}
