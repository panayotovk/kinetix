package com.kinetix.risk.client

import com.kinetix.common.model.InstrumentId
import com.kinetix.risk.client.dtos.InstrumentDto

interface InstrumentServiceClient {
    suspend fun getInstrument(instrumentId: InstrumentId): ClientResponse<InstrumentDto>
    suspend fun getInstruments(instrumentIds: List<InstrumentId>): Map<String, InstrumentDto>
}
