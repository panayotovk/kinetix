package com.kinetix.notification.delivery

import com.kinetix.notification.model.AlertEvent
import com.kinetix.notification.model.DeliveryChannel
import org.slf4j.LoggerFactory

class PagerDutyDeliveryService : DeliveryService {
    private val logger = LoggerFactory.getLogger(PagerDutyDeliveryService::class.java)
    private val _sentAlerts = mutableListOf<AlertEvent>()
    val sentAlerts: List<AlertEvent> get() = _sentAlerts.toList()

    override val channel: DeliveryChannel = DeliveryChannel.PAGER_DUTY

    override suspend fun deliver(event: AlertEvent) {
        _sentAlerts.add(event)
        logger.info(
            "PagerDuty alert sent: rule={}, severity={}, book={}",
            event.ruleName, event.severity, event.bookId,
        )
        // TODO(ALT-04): integrate with real PagerDuty Events API v2 when credentials are available
    }
}
