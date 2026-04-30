package com.kinetix.risk.grpc

import com.kinetix.proto.risk.GreeksRequest
import com.kinetix.proto.risk.GreeksResponse
import com.kinetix.proto.risk.HistoricalReplayRequest
import com.kinetix.proto.risk.HistoricalReplayResponse
import com.kinetix.proto.risk.ListScenariosRequest
import com.kinetix.proto.risk.ListScenariosResponse
import com.kinetix.proto.risk.ReverseStressRequest
import com.kinetix.proto.risk.ReverseStressResponse
import com.kinetix.proto.risk.StressTestRequest
import com.kinetix.proto.risk.StressTestResponse
import com.kinetix.proto.risk.StressTestServiceGrpcKt
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test double for the gRPC `StressTestService`. Configure responses by supplying
 * a lambda for each RPC; received requests are exposed through the `*Requests`
 * lists so tests can assert on what the orchestrator actually sent.
 *
 * Bind to a real gRPC server via [GrpcFakeServer] so calls travel over real
 * HTTP/2 — interceptors, marshalling, and channel wiring are all exercised.
 */
class FakeStressTestService(
    var runStressTestHandler: (StressTestRequest) -> StressTestResponse =
        { StressTestResponse.getDefaultInstance() },
    var listScenariosHandler: (ListScenariosRequest) -> ListScenariosResponse =
        { ListScenariosResponse.getDefaultInstance() },
    var runHistoricalReplayHandler: (HistoricalReplayRequest) -> HistoricalReplayResponse =
        { HistoricalReplayResponse.getDefaultInstance() },
    var runReverseStressHandler: (ReverseStressRequest) -> ReverseStressResponse =
        { ReverseStressResponse.getDefaultInstance() },
    var calculateGreeksHandler: (GreeksRequest) -> GreeksResponse =
        { GreeksResponse.getDefaultInstance() },
) : StressTestServiceGrpcKt.StressTestServiceCoroutineImplBase() {

    val runStressTestRequests: MutableList<StressTestRequest> = CopyOnWriteArrayList()
    val listScenariosRequests: MutableList<ListScenariosRequest> = CopyOnWriteArrayList()
    val runHistoricalReplayRequests: MutableList<HistoricalReplayRequest> = CopyOnWriteArrayList()
    val runReverseStressRequests: MutableList<ReverseStressRequest> = CopyOnWriteArrayList()
    val calculateGreeksRequests: MutableList<GreeksRequest> = CopyOnWriteArrayList()

    override suspend fun runStressTest(request: StressTestRequest): StressTestResponse {
        runStressTestRequests += request
        return runStressTestHandler(request)
    }

    override suspend fun listScenarios(request: ListScenariosRequest): ListScenariosResponse {
        listScenariosRequests += request
        return listScenariosHandler(request)
    }

    override suspend fun runHistoricalReplay(request: HistoricalReplayRequest): HistoricalReplayResponse {
        runHistoricalReplayRequests += request
        return runHistoricalReplayHandler(request)
    }

    override suspend fun runReverseStress(request: ReverseStressRequest): ReverseStressResponse {
        runReverseStressRequests += request
        return runReverseStressHandler(request)
    }

    override suspend fun calculateGreeks(request: GreeksRequest): GreeksResponse {
        calculateGreeksRequests += request
        return calculateGreeksHandler(request)
    }
}
