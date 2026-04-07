package com.kinetix.risk.service

import com.kinetix.common.model.InstrumentId
import com.kinetix.common.resilience.CircuitBreaker
import com.kinetix.common.resilience.CircuitBreakerOpenException
import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.PriceServiceClient
import com.kinetix.risk.client.RatesServiceClient
import com.kinetix.risk.client.ReferenceDataServiceClient
import com.kinetix.risk.client.VolatilityServiceClient
import com.kinetix.risk.client.CorrelationServiceClient
import com.kinetix.risk.model.CurveMarketData
import com.kinetix.risk.model.CurvePointValue
import com.kinetix.risk.model.DiscoveredDependency
import com.kinetix.risk.model.FetchFailure
import com.kinetix.risk.model.FetchResult
import com.kinetix.risk.model.FetchSuccess
import com.kinetix.risk.model.MarketDataValue
import com.kinetix.risk.model.MatrixMarketData
import com.kinetix.risk.model.ScalarMarketData
import com.kinetix.risk.model.TimeSeriesMarketData
import com.kinetix.risk.model.TimeSeriesPoint
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Currency

class MarketDataFetcher(
    private val priceServiceClient: PriceServiceClient,
    private val ratesServiceClient: RatesServiceClient? = null,
    private val referenceDataServiceClient: ReferenceDataServiceClient? = null,
    private val volatilityServiceClient: VolatilityServiceClient? = null,
    private val correlationServiceClient: CorrelationServiceClient? = null,
    private val priceServiceBaseUrl: String = "",
    private val ratesServiceBaseUrl: String = "",
    private val referenceDataServiceBaseUrl: String = "",
    private val volatilityServiceBaseUrl: String = "",
    private val correlationServiceBaseUrl: String = "",
    private val priceCircuitBreaker: CircuitBreaker? = null,
    private val ratesCircuitBreaker: CircuitBreaker? = null,
    private val referenceDataCircuitBreaker: CircuitBreaker? = null,
    private val volatilityCircuitBreaker: CircuitBreaker? = null,
    private val correlationCircuitBreaker: CircuitBreaker? = null,
) {
    private val logger = LoggerFactory.getLogger(MarketDataFetcher::class.java)

    private fun circuitBreakerFor(dataType: String): CircuitBreaker? = when (dataType) {
        "SPOT_PRICE", "HISTORICAL_PRICES" -> priceCircuitBreaker
        "YIELD_CURVE", "RISK_FREE_RATE", "FORWARD_CURVE" -> ratesCircuitBreaker
        "DIVIDEND_YIELD", "CREDIT_SPREAD" -> referenceDataCircuitBreaker
        "VOLATILITY_SURFACE" -> volatilityCircuitBreaker
        "CORRELATION_MATRIX" -> correlationCircuitBreaker
        else -> null
    }

    private val concurrencyLimit = Semaphore(10)

    suspend fun fetch(dependencies: List<DiscoveredDependency>, correlationId: String? = null): List<FetchResult> {
        val results = coroutineScope {
            dependencies.map { dep ->
                async { concurrencyLimit.withPermit { fetchOneDependency(dep, correlationId) } }
            }.awaitAll()
        }

        val successCount = results.count { it is FetchSuccess }
        logger.debug("Fetched {} market data values for {} dependencies", successCount, dependencies.size)
        return results
    }

    private suspend fun fetchOneDependency(dep: DiscoveredDependency, correlationId: String?): FetchResult {
        val startTime = Instant.now()
        val circuitBreaker = circuitBreakerFor(dep.dataType)
        return try {
            val result = if (circuitBreaker != null) {
                circuitBreaker.execute {
                    val r = fetchDependency(dep.dataType, dep.instrumentId, dep.assetClass, dep.parameters)
                    if (r is ClientResponse.ServiceUnavailable || r is ClientResponse.UpstreamError || r is ClientResponse.NetworkError) {
                        throw MarketDataFetchException(dep.dataType, r)
                    }
                    r
                }
            } else {
                fetchDependency(dep.dataType, dep.instrumentId, dep.assetClass, dep.parameters)
            }
            val durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis()
            resultToFetchResult(result, dep, startTime, durationMs, correlationId)
        } catch (e: CircuitBreakerOpenException) {
            val durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis()
            logger.debug("Circuit breaker open for {}, skipping fetch of {} for {}", e.circuitName, dep.dataType, dep.instrumentId)
            FetchFailure(dep, "CIRCUIT_OPEN", resolveUrl(dep.dataType, dep.instrumentId, dep.parameters), null, "Circuit breaker '${e.circuitName}' is open", resolveServiceName(dep.dataType), startTime, durationMs, correlationId)
        } catch (e: MarketDataFetchException) {
            val durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis()
            marketDataExceptionToFetchResult(e, dep, startTime, durationMs, correlationId)
        } catch (e: Exception) {
            val durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis()
            logger.warn("Failed to fetch {} for {}, skipping", dep.dataType, dep.instrumentId, e)
            FetchFailure(dep, "EXCEPTION", resolveUrl(dep.dataType, dep.instrumentId, dep.parameters), extractHttpStatus(e), e.message, resolveServiceName(dep.dataType), startTime, durationMs, correlationId)
        }
    }

    private fun resultToFetchResult(result: ClientResponse<MarketDataValue>?, dep: DiscoveredDependency, startTime: Instant, durationMs: Long, correlationId: String?): FetchResult {
        val url = resolveUrl(dep.dataType, dep.instrumentId, dep.parameters)
        val service = resolveServiceName(dep.dataType)
        return when (result) {
            is ClientResponse.Success -> FetchSuccess(dep, result.value)
            is ClientResponse.NotFound -> FetchFailure(dep, "NOT_FOUND", url, result.httpStatus, null, service, startTime, durationMs, correlationId)
            is ClientResponse.ServiceUnavailable -> {
                logger.warn("Service unavailable fetching {} for {}", dep.dataType, dep.instrumentId)
                FetchFailure(dep, "SERVICE_UNAVAILABLE", url, 503, null, service, startTime, durationMs, correlationId)
            }
            is ClientResponse.UpstreamError -> {
                logger.warn("Upstream error {} fetching {} for {}: {}", result.httpStatus, dep.dataType, dep.instrumentId, result.message)
                FetchFailure(dep, "UPSTREAM_ERROR", url, result.httpStatus, result.message, service, startTime, durationMs, correlationId)
            }
            is ClientResponse.NetworkError -> {
                logger.warn("Network error fetching {} for {}", dep.dataType, dep.instrumentId, result.cause)
                FetchFailure(dep, "NETWORK_ERROR", url, null, result.cause.message, service, startTime, durationMs, correlationId)
            }
            null -> {
                val reason = if (isClientAvailable(dep.dataType)) "NOT_FOUND" else "CLIENT_UNAVAILABLE"
                FetchFailure(dep, reason, url, null, null, service, startTime, durationMs, correlationId)
            }
        }
    }

    private fun marketDataExceptionToFetchResult(e: MarketDataFetchException, dep: DiscoveredDependency, startTime: Instant, durationMs: Long, correlationId: String?): FetchResult {
        val url = resolveUrl(dep.dataType, dep.instrumentId, dep.parameters)
        val service = resolveServiceName(dep.dataType)
        return when (val r = e.response) {
            is ClientResponse.ServiceUnavailable -> {
                logger.warn("Service unavailable fetching {} for {}", dep.dataType, dep.instrumentId)
                FetchFailure(dep, "SERVICE_UNAVAILABLE", url, 503, null, service, startTime, durationMs, correlationId)
            }
            is ClientResponse.UpstreamError -> {
                logger.warn("Upstream error {} fetching {} for {}: {}", r.httpStatus, dep.dataType, dep.instrumentId, r.message)
                FetchFailure(dep, "UPSTREAM_ERROR", url, r.httpStatus, r.message, service, startTime, durationMs, correlationId)
            }
            is ClientResponse.NetworkError -> {
                logger.warn("Network error fetching {} for {}", dep.dataType, dep.instrumentId, r.cause)
                FetchFailure(dep, "NETWORK_ERROR", url, null, r.cause.message, service, startTime, durationMs, correlationId)
            }
            else -> FetchFailure(dep, "EXCEPTION", url, null, null, service, startTime, durationMs, correlationId)
        }
    }

    private fun isClientAvailable(dataType: String): Boolean = when (dataType) {
        "SPOT_PRICE", "HISTORICAL_PRICES" -> true
        "YIELD_CURVE", "RISK_FREE_RATE", "FORWARD_CURVE" -> ratesServiceClient != null
        "DIVIDEND_YIELD", "CREDIT_SPREAD" -> referenceDataServiceClient != null
        "VOLATILITY_SURFACE" -> volatilityServiceClient != null
        "CORRELATION_MATRIX" -> correlationServiceClient != null
        else -> false
    }

    private fun resolveServiceName(dataType: String): String = when (dataType) {
        "SPOT_PRICE", "HISTORICAL_PRICES" -> "price-service"
        "YIELD_CURVE", "RISK_FREE_RATE", "FORWARD_CURVE" -> "rates-service"
        "DIVIDEND_YIELD", "CREDIT_SPREAD" -> "reference-data-service"
        "VOLATILITY_SURFACE" -> "volatility-service"
        "CORRELATION_MATRIX" -> "correlation-service"
        else -> "unknown"
    }

    private fun resolveUrl(dataType: String, instrumentId: String, parameters: Map<String, String>): String? {
        val id = instrumentId
        return when (dataType) {
            "SPOT_PRICE" -> "$priceServiceBaseUrl/api/prices/$id/latest"
            "HISTORICAL_PRICES" -> "$priceServiceBaseUrl/api/prices/$id/history"
            "YIELD_CURVE" -> {
                val curveId = parameters["curveId"] ?: id
                "$ratesServiceBaseUrl/api/rates/yield-curves/$curveId/latest"
            }
            "RISK_FREE_RATE" -> {
                val currency = parameters["currency"] ?: "USD"
                val tenor = parameters["tenor"] ?: "3M"
                "$ratesServiceBaseUrl/api/rates/risk-free/$currency/$tenor"
            }
            "FORWARD_CURVE" -> "$ratesServiceBaseUrl/api/rates/forward-curves/$id/latest"
            "DIVIDEND_YIELD" -> "$referenceDataServiceBaseUrl/api/reference-data/$id/dividend-yield"
            "CREDIT_SPREAD" -> "$referenceDataServiceBaseUrl/api/reference-data/$id/credit-spread"
            "VOLATILITY_SURFACE" -> "$volatilityServiceBaseUrl/api/volatility/$id/surface/latest"
            "CORRELATION_MATRIX" -> "$correlationServiceBaseUrl/api/correlation/matrix"
            else -> null
        }
    }

    private fun extractHttpStatus(e: Exception): Int? {
        if (e is ResponseException) {
            return e.response.status.value
        }
        return null
    }

    private suspend fun fetchDependency(
        dataType: String,
        instrumentId: String,
        assetClass: String,
        parameters: Map<String, String>,
    ): ClientResponse<MarketDataValue>? = when (dataType) {
        "SPOT_PRICE" -> {
            when (val response = priceServiceClient.getLatestPrice(InstrumentId(instrumentId))) {
                is ClientResponse.Success -> ClientResponse.Success(
                    ScalarMarketData(
                        dataType = "SPOT_PRICE",
                        instrumentId = instrumentId,
                        assetClass = assetClass,
                        value = response.value.price.amount.toDouble(),
                    )
                )
                else -> response as ClientResponse<Nothing>
            }
        }

        "HISTORICAL_PRICES" -> {
            val lookbackDays = parameters["lookbackDays"]?.toLongOrNull() ?: 252L
            val now = Instant.now()
            val from = now.minus(lookbackDays, ChronoUnit.DAYS)
            when (val response = priceServiceClient.getPriceHistory(InstrumentId(instrumentId), from, now, interval = "1d")) {
                is ClientResponse.Success -> {
                    val history = response.value
                    if (history.isNotEmpty()) {
                        ClientResponse.Success(
                            TimeSeriesMarketData(
                                dataType = "HISTORICAL_PRICES",
                                instrumentId = instrumentId,
                                assetClass = assetClass,
                                points = history.sortedBy { it.timestamp }.map { pp ->
                                    TimeSeriesPoint(
                                        timestamp = pp.timestamp,
                                        value = pp.price.amount.toDouble(),
                                    )
                                },
                            )
                        )
                    } else null
                }
                else -> response as ClientResponse<Nothing>
            }
        }

        "YIELD_CURVE" -> {
            val curveId = parameters["curveId"] ?: instrumentId
            val response = ratesServiceClient?.getLatestYieldCurve(curveId)
            when (response) {
                is ClientResponse.Success -> ClientResponse.Success(
                    CurveMarketData(
                        dataType = "YIELD_CURVE",
                        instrumentId = instrumentId,
                        assetClass = assetClass,
                        points = response.value.tenors.map { tenor ->
                            CurvePointValue(tenor = tenor.label, value = tenor.rate.toDouble())
                        },
                    )
                )
                null -> null
                else -> response as ClientResponse<Nothing>
            }
        }

        "RISK_FREE_RATE" -> {
            val currency = parameters["currency"] ?: "USD"
            val tenor = parameters["tenor"] ?: "3M"
            val response = ratesServiceClient?.getLatestRiskFreeRate(Currency.getInstance(currency), tenor)
            when (response) {
                is ClientResponse.Success -> ClientResponse.Success(
                    ScalarMarketData(
                        dataType = "RISK_FREE_RATE",
                        instrumentId = instrumentId,
                        assetClass = assetClass,
                        value = response.value.rate,
                    )
                )
                null -> null
                else -> response as ClientResponse<Nothing>
            }
        }

        "FORWARD_CURVE" -> {
            val response = ratesServiceClient?.getLatestForwardCurve(InstrumentId(instrumentId))
            when (response) {
                is ClientResponse.Success -> ClientResponse.Success(
                    CurveMarketData(
                        dataType = "FORWARD_CURVE",
                        instrumentId = instrumentId,
                        assetClass = assetClass,
                        points = response.value.points.map { point ->
                            CurvePointValue(tenor = point.tenor, value = point.value)
                        },
                    )
                )
                null -> null
                else -> response as ClientResponse<Nothing>
            }
        }

        "DIVIDEND_YIELD" -> {
            val response = referenceDataServiceClient?.getLatestDividendYield(InstrumentId(instrumentId))
            when (response) {
                is ClientResponse.Success -> ClientResponse.Success(
                    ScalarMarketData(
                        dataType = "DIVIDEND_YIELD",
                        instrumentId = instrumentId,
                        assetClass = assetClass,
                        value = response.value.yield,
                    )
                )
                null -> null
                else -> response as ClientResponse<Nothing>
            }
        }

        "CREDIT_SPREAD" -> {
            val response = referenceDataServiceClient?.getLatestCreditSpread(InstrumentId(instrumentId))
            when (response) {
                is ClientResponse.Success -> ClientResponse.Success(
                    ScalarMarketData(
                        dataType = "CREDIT_SPREAD",
                        instrumentId = instrumentId,
                        assetClass = assetClass,
                        value = response.value.spread,
                    )
                )
                null -> null
                else -> response as ClientResponse<Nothing>
            }
        }

        "VOLATILITY_SURFACE" -> {
            val response = volatilityServiceClient?.getLatestSurface(InstrumentId(instrumentId))
            when (response) {
                is ClientResponse.Success -> {
                    val surface = response.value
                    val strikes = surface.strikes.map { s -> s.toDouble() }
                    val maturities = surface.maturities.map { m -> m.toString() }
                    val values = maturities.flatMap { mat ->
                        strikes.map { strike ->
                            surface.volAt(strike.toBigDecimal(), mat.toInt()).toDouble()
                        }
                    }
                    ClientResponse.Success(
                        MatrixMarketData(
                            dataType = "VOLATILITY_SURFACE",
                            instrumentId = instrumentId,
                            assetClass = assetClass,
                            rows = maturities,
                            columns = strikes.map { s -> s.toString() },
                            values = values,
                        )
                    )
                }
                null -> null
                else -> response as ClientResponse<Nothing>
            }
        }

        "CORRELATION_MATRIX" -> {
            val labels = parameters["labels"]?.split(",") ?: emptyList()
            val windowDays = parameters["windowDays"]?.toIntOrNull() ?: 252
            if (labels.isEmpty()) {
                logger.debug("No labels provided for CORRELATION_MATRIX, skipping")
                null
            } else {
                val response = correlationServiceClient?.getCorrelationMatrix(labels, windowDays)
                when (response) {
                    is ClientResponse.Success -> ClientResponse.Success(
                        MatrixMarketData(
                            dataType = "CORRELATION_MATRIX",
                            instrumentId = instrumentId,
                            assetClass = assetClass,
                            rows = response.value.labels,
                            columns = response.value.labels,
                            values = response.value.values,
                        )
                    )
                    null -> null
                    else -> response as ClientResponse<Nothing>
                }
            }
        }

        else -> {
            logger.debug("Cannot fetch {} for {}, skipping", dataType, instrumentId)
            null
        }
    }
}
