package com.kinetix.position.persistence

import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position

interface PositionRepository {
    suspend fun save(position: Position)
    suspend fun findByBookId(portfolioId: BookId): List<Position>
    suspend fun findByKey(portfolioId: BookId, instrumentId: InstrumentId): Position?
    suspend fun findByInstrumentId(instrumentId: InstrumentId): List<Position>
    suspend fun delete(portfolioId: BookId, instrumentId: InstrumentId)
    suspend fun findDistinctBookIds(): List<BookId>
}
