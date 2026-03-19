package com.kinetix.position.service

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position
import com.kinetix.position.persistence.PositionRepository

data class GetPositionsQuery(
    val bookId: BookId,
)

class PositionQueryService(
    private val positionRepository: PositionRepository,
) {
    suspend fun handle(query: GetPositionsQuery): List<Position> {
        return positionRepository.findByBookId(query.bookId)
    }
}
