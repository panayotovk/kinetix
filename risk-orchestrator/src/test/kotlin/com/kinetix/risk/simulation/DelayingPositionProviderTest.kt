package com.kinetix.risk.simulation

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position
import com.kinetix.risk.client.PositionProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.system.measureTimeMillis

class DelayingPositionProviderTest : FunSpec({

    val delegate = mockk<PositionProvider>()
    val delayRange = 100L..200L
    val provider = DelayingPositionProvider(delegate, delayRange)

    val bookId = BookId("port-1")
    val positions = listOf(mockk<Position>())

    test("delegates getPositions to underlying provider and returns result") {
        coEvery { delegate.getPositions(bookId) } returns positions

        val result = provider.getPositions(bookId)

        result shouldBe positions
    }

    test("applies delay before returning") {
        coEvery { delegate.getPositions(bookId) } returns positions

        val elapsed = measureTimeMillis {
            provider.getPositions(bookId)
        }

        elapsed shouldBeGreaterThanOrEqual delayRange.first
    }
})
