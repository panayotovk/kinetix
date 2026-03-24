package com.kinetix.risk.service

import com.kinetix.risk.model.IntradayPnlSnapshot

interface IntradayPnlPublisher {
    suspend fun publish(snapshot: IntradayPnlSnapshot)
}
