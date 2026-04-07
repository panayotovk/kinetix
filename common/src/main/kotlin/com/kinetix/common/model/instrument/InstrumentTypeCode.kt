package com.kinetix.common.model.instrument

enum class InstrumentTypeCode {
    CASH_EQUITY,
    EQUITY_OPTION,
    EQUITY_FUTURE,
    GOVERNMENT_BOND,
    CORPORATE_BOND,
    FX_SPOT,
    FX_FORWARD,
    FX_OPTION,
    COMMODITY_FUTURE,
    COMMODITY_OPTION,
    INTEREST_RATE_SWAP;

    companion object {
        fun fromString(value: String?): InstrumentTypeCode? = when {
            value == null -> null
            value == "UNKNOWN" -> null
            else -> entries.find { it.name == value }
        }
    }
}
