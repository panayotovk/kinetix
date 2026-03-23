package com.kinetix.position.service

import com.kinetix.position.model.LimitBreachResult

interface PreTradeCheckService {
    suspend fun check(command: BookTradeCommand): LimitBreachResult
}
