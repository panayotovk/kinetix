package com.kinetix.risk.client

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position

interface PositionServiceClient {
    suspend fun getPositions(portfolioId: BookId): ClientResponse<List<Position>>
    suspend fun getDistinctBookIds(): ClientResponse<List<BookId>>
}
