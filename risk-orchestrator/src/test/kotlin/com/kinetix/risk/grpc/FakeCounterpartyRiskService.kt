package com.kinetix.risk.grpc

import com.kinetix.proto.risk.CalculateCVARequest
import com.kinetix.proto.risk.CalculateCVAResponse
import com.kinetix.proto.risk.CalculatePFERequest
import com.kinetix.proto.risk.CalculatePFEResponse
import com.kinetix.proto.risk.CounterpartyRiskServiceGrpcKt
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test double for the gRPC `CounterpartyRiskService`. Configure responses by supplying
 * handler lambdas; received requests are exposed through the `*Requests` lists so tests
 * can assert on what the orchestrator actually sent.
 *
 * Bind to a real gRPC server via [GrpcFakeServer] so calls travel over real HTTP/2
 * — interceptors, marshalling, and channel wiring are all exercised.
 */
class FakeCounterpartyRiskService(
    var calculatePFEHandler: (CalculatePFERequest) -> CalculatePFEResponse =
        { CalculatePFEResponse.getDefaultInstance() },
    var calculateCVAHandler: (CalculateCVARequest) -> CalculateCVAResponse =
        { CalculateCVAResponse.getDefaultInstance() },
) : CounterpartyRiskServiceGrpcKt.CounterpartyRiskServiceCoroutineImplBase() {

    val calculatePFERequests: MutableList<CalculatePFERequest> = CopyOnWriteArrayList()
    val calculateCVARequests: MutableList<CalculateCVARequest> = CopyOnWriteArrayList()

    override suspend fun calculatePFE(request: CalculatePFERequest): CalculatePFEResponse {
        calculatePFERequests += request
        return calculatePFEHandler(request)
    }

    override suspend fun calculateCVA(request: CalculateCVARequest): CalculateCVAResponse {
        calculateCVARequests += request
        return calculateCVAHandler(request)
    }
}
