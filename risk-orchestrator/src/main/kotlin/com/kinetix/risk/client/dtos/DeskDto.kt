package com.kinetix.risk.client.dtos

import com.kinetix.common.model.Desk
import com.kinetix.common.model.DeskId
import com.kinetix.common.model.DivisionId
import kotlinx.serialization.Serializable

@Serializable
data class DeskDto(
    val id: String,
    val name: String,
    val divisionId: String,
) {
    fun toDomain() = Desk(
        id = DeskId(id),
        name = name,
        divisionId = DivisionId(divisionId),
    )
}
