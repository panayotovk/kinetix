package com.kinetix.risk.client.dtos

import com.kinetix.common.model.BookId
import kotlinx.serialization.Serializable

@Serializable
data class BookSummaryDto(
    val bookId: String,
) {
    fun toDomain(): BookId = BookId(bookId)
}
