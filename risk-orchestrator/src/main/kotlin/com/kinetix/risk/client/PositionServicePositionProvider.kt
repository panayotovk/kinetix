package com.kinetix.risk.client

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position

class PositionServicePositionProvider(
    private val positionServiceClient: PositionServiceClient,
) : PositionProvider {

    override suspend fun getPositions(bookId: BookId): List<Position> {
        return when (val response = positionServiceClient.getPositions(bookId)) {
            is ClientResponse.Success -> response.value
            is ClientResponse.NotFound -> emptyList()
        }
    }
}
