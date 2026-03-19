package com.kinetix.risk.simulation

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position
import com.kinetix.risk.client.PositionProvider
import kotlinx.coroutines.delay

class DelayingPositionProvider(
    private val delegate: PositionProvider,
    private val delayMs: LongRange,
) : PositionProvider {

    override suspend fun getPositions(bookId: BookId): List<Position> {
        delay(delayMs.random())
        return delegate.getPositions(bookId)
    }
}
