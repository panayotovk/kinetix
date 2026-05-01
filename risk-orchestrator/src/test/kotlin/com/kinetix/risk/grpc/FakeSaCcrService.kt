package com.kinetix.risk.grpc

import com.kinetix.proto.risk.CalculateSaCcrRequest
import com.kinetix.proto.risk.CalculateSaCcrResponse
import com.kinetix.proto.risk.SaCcrServiceGrpcKt
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test double for the gRPC `SaCcrService`. Configure the response by supplying a
 * handler lambda; received requests are exposed through [calculateSaCcrRequests]
 * so tests can assert on what the orchestrator actually sent.
 *
 * Bind to a real gRPC server via [GrpcFakeServer] so calls travel over real
 * HTTP/2 — interceptors, marshalling, and channel wiring are all exercised.
 */
class FakeSaCcrService(
    var calculateSaCcrHandler: (CalculateSaCcrRequest) -> CalculateSaCcrResponse =
        { CalculateSaCcrResponse.getDefaultInstance() },
) : SaCcrServiceGrpcKt.SaCcrServiceCoroutineImplBase() {

    val calculateSaCcrRequests: MutableList<CalculateSaCcrRequest> = CopyOnWriteArrayList()

    override suspend fun calculateSaCcr(request: CalculateSaCcrRequest): CalculateSaCcrResponse {
        calculateSaCcrRequests += request
        return calculateSaCcrHandler(request)
    }
}
