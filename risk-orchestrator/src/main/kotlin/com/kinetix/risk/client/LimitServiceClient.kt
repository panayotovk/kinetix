package com.kinetix.risk.client

import com.kinetix.risk.client.dtos.LimitDefinitionDto

interface LimitServiceClient {
    suspend fun getLimits(): ClientResponse<List<LimitDefinitionDto>>
}
