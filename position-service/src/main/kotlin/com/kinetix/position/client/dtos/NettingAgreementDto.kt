package com.kinetix.position.client.dtos

import com.kinetix.position.client.NettingAgreement
import kotlinx.serialization.Serializable

@Serializable
data class NettingAgreementDto(
    val nettingSetId: String,
    val counterpartyId: String,
    val agreementType: String,
    val closeOutNetting: Boolean,
    val csaThreshold: Double? = null,
    val currency: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toDomain() = NettingAgreement(
        nettingSetId = nettingSetId,
        counterpartyId = counterpartyId,
    )
}
