package com.kinetix.risk.client.dtos

import com.kinetix.common.model.Division
import com.kinetix.common.model.DivisionId
import kotlinx.serialization.Serializable

@Serializable
data class DivisionDto(
    val id: String,
    val name: String,
) {
    fun toDomain() = Division(
        id = DivisionId(id),
        name = name,
    )
}
