package com.kinetix.notification.persistence

import com.kinetix.notification.model.AlertEvent
import com.kinetix.notification.model.AlertStatus

interface AlertEventRepository {
    suspend fun save(event: AlertEvent)
    suspend fun findRecent(limit: Int = 50, status: AlertStatus? = null): List<AlertEvent>
    suspend fun findActiveByRuleAndBook(ruleId: String, bookId: String): AlertEvent?
    suspend fun findActiveByBook(bookId: String): List<AlertEvent>
    suspend fun updateStatus(id: String, status: AlertStatus, resolvedAt: java.time.Instant? = null, resolvedReason: String? = null)
    suspend fun findById(id: String): AlertEvent?
}
