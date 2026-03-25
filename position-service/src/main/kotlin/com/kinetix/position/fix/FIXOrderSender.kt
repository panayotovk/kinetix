package com.kinetix.position.fix

interface FIXOrderSender {
    suspend fun send(order: Order, session: FIXSession)
}
