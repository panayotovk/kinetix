package com.kinetix.gateway.client

import com.kinetix.gateway.dtos.VolSurfaceDiffResponse
import com.kinetix.gateway.dtos.VolSurfaceResponse

interface VolatilityServiceClient {
    suspend fun getSurface(instrumentId: String): VolSurfaceResponse?
    suspend fun getSurfaceDiff(instrumentId: String, compareDate: String): VolSurfaceDiffResponse?
}
