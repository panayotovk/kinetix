package com.kinetix.position.service

import com.kinetix.position.client.NettingAgreement
import com.kinetix.position.client.ReferenceDataServiceClient
import com.kinetix.position.persistence.NettingSetTradeRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.*

class NettingSetAssignerTest : FunSpec({

    val referenceDataClient = mockk<ReferenceDataServiceClient>()
    val nettingSetTradeRepository = mockk<NettingSetTradeRepository>()
    val assigner = NettingSetAssigner(referenceDataClient, nettingSetTradeRepository)

    beforeEach {
        clearMocks(referenceDataClient, nettingSetTradeRepository)
    }

    test("assigns trade to netting set when counterpartyId has netting agreement") {
        val agreement = NettingAgreement(nettingSetId = "ns-1", counterpartyId = "cpty-abc")
        coEvery { referenceDataClient.getNettingAgreementsForCounterparty("cpty-abc") } returns listOf(agreement)
        coEvery { nettingSetTradeRepository.assign(any(), any()) } just runs

        assigner.assignIfApplicable(tradeId = "trade-42", counterpartyId = "cpty-abc")

        coVerify(exactly = 1) { nettingSetTradeRepository.assign("trade-42", "ns-1") }
    }

    test("skips assignment when counterpartyId is null") {
        assigner.assignIfApplicable(tradeId = "trade-42", counterpartyId = null)

        coVerify(exactly = 0) { referenceDataClient.getNettingAgreementsForCounterparty(any()) }
        coVerify(exactly = 0) { nettingSetTradeRepository.assign(any(), any()) }
    }

    test("skips assignment when no netting agreements found") {
        coEvery { referenceDataClient.getNettingAgreementsForCounterparty("cpty-xyz") } returns emptyList()

        assigner.assignIfApplicable(tradeId = "trade-42", counterpartyId = "cpty-xyz")

        coVerify(exactly = 0) { nettingSetTradeRepository.assign(any(), any()) }
    }

    test("does not fail trade booking when netting set assignment throws") {
        coEvery { referenceDataClient.getNettingAgreementsForCounterparty("cpty-abc") } throws RuntimeException("ref-data unavailable")

        assigner.assignIfApplicable(tradeId = "trade-42", counterpartyId = "cpty-abc")

        coVerify(exactly = 0) { nettingSetTradeRepository.assign(any(), any()) }
    }
})
