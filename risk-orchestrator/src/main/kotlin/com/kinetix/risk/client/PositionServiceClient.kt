package com.kinetix.risk.client

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position

interface PositionServiceClient {
    suspend fun getPositions(bookId: BookId): ClientResponse<List<Position>>
    suspend fun getDistinctBookIds(): ClientResponse<List<BookId>>
}
