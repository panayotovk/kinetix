package com.kinetix.position.service

import com.kinetix.position.persistence.FxRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.concurrent.ConcurrentHashMap

class LiveFxRateProvider(
    private val delegate: FxRateProvider,
    private val repository: FxRateRepository? = null,
) : FxRateProvider {

    private val liveRates = ConcurrentHashMap<Pair<Currency, Currency>, BigDecimal>()
    private val persistenceScope = CoroutineScope(Dispatchers.IO)

    fun onPriceUpdate(instrumentId: String, priceAmount: BigDecimal) {
        if (instrumentId.length != 6) return

        val baseCurrencyCode = instrumentId.substring(0, 3)
        val quoteCurrencyCode = instrumentId.substring(3, 6)

        val base = try {
            Currency.getInstance(baseCurrencyCode)
        } catch (_: IllegalArgumentException) {
            return
        }
        val quote = try {
            Currency.getInstance(quoteCurrencyCode)
        } catch (_: IllegalArgumentException) {
            return
        }

        liveRates[base to quote] = priceAmount

        repository?.let { repo ->
            persistenceScope.launch {
                repo.upsert(base, quote, priceAmount)
            }
        }
    }

    override suspend fun getRate(from: Currency, to: Currency): BigDecimal? {
        if (from == to) return BigDecimal.ONE

        liveRates[from to to]?.let { return it }

        liveRates[to to from]?.let { reverse ->
            return BigDecimal.ONE.divide(reverse, 10, RoundingMode.HALF_UP)
        }

        repository?.findRate(from, to)?.let { return it }

        return delegate.getRate(from, to)
    }
}
