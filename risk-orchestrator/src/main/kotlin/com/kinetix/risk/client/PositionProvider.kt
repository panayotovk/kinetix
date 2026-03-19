package com.kinetix.risk.client

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position

interface PositionProvider {
    suspend fun getPositions(bookId: BookId): List<Position>
}
