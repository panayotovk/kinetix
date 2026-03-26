package com.kinetix.risk.service

import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.Money
import com.kinetix.common.model.PricePoint
import com.kinetix.common.model.PriceSource
import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.PriceServiceClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

private val USD = Currency.getInstance("USD")

private fun pricePoint(instrumentId: String, amount: String) = PricePoint(
    instrumentId = InstrumentId(instrumentId),
    price = Money(BigDecimal(amount), USD),
    timestamp = Instant.now(),
    source = PriceSource.INTERNAL,
)

class PriceBasedFxRateProviderTest : FunSpec({

    test("returns ONE for same currency") {
        val client = mockk<PriceServiceClient>()
        val provider = PriceBasedFxRateProvider(client)

        val rate = provider.getRate("USD", "USD")

        rate shouldBe BigDecimal.ONE
        coVerify(exactly = 0) { client.getLatestPrice(any()) }
    }

    test("returns direct rate when pair exists") {
        val client = mockk<PriceServiceClient>()
        coEvery { client.getLatestPrice(InstrumentId("EURUSD")) } returns
            ClientResponse.Success(pricePoint("EURUSD", "1.08"))
        val provider = PriceBasedFxRateProvider(client)

        val rate = provider.getRate("EUR", "USD")

        rate shouldBe BigDecimal("1.08")
    }

    test("returns inverse rate when direct not found") {
        val client = mockk<PriceServiceClient>()
        coEvery { client.getLatestPrice(InstrumentId("EURUSD")) } returns
            ClientResponse.NotFound(404)
        coEvery { client.getLatestPrice(InstrumentId("USDEUR")) } returns
            ClientResponse.Success(pricePoint("USDEUR", "0.5"))
        val provider = PriceBasedFxRateProvider(client)

        val rate = provider.getRate("EUR", "USD")

        // inverse of 0.5 = 2.0
        rate!!.compareTo(BigDecimal("2.0")) shouldBe 0
    }

    test("returns null when both directions not found") {
        val client = mockk<PriceServiceClient>()
        coEvery { client.getLatestPrice(InstrumentId("GBPUSD")) } returns
            ClientResponse.NotFound(404)
        coEvery { client.getLatestPrice(InstrumentId("USDGBP")) } returns
            ClientResponse.NotFound(404)
        val provider = PriceBasedFxRateProvider(client)

        val rate = provider.getRate("GBP", "USD")

        rate.shouldBeNull()
    }

    test("returns null on exception from price service") {
        val client = mockk<PriceServiceClient>()
        coEvery { client.getLatestPrice(any()) } throws RuntimeException("price service unavailable")
        val provider = PriceBasedFxRateProvider(client)

        val rate = provider.getRate("GBP", "USD")

        rate.shouldBeNull()
    }
})
