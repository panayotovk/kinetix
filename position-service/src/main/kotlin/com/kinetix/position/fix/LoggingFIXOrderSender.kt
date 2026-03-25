package com.kinetix.position.fix

import com.kinetix.common.model.Side
import org.slf4j.LoggerFactory

/**
 * A FIXOrderSender that logs outbound NewOrderSingle messages without requiring a real FIX
 * session. Used in development and as the default wiring until a live FIX adapter is deployed.
 *
 * The log line reproduces the key FIX tags so that operators can correlate log output with
 * downstream broker confirmations during testing and integration:
 *   35=D  MsgType = NewOrderSingle
 *   11    ClOrdID (our internal order ID)
 *   55    Symbol (instrument)
 *   54    Side (1=Buy, 2=Sell)
 *   38    OrderQty
 *   40    OrdType (1=Market, 2=Limit)
 *   49    SenderCompID (FIX session counterparty)
 */
class LoggingFIXOrderSender : FIXOrderSender {

    private val logger = LoggerFactory.getLogger(LoggingFIXOrderSender::class.java)

    override suspend fun send(order: Order, session: FIXSession) {
        logger.info(
            "FIX NewOrderSingle: 35=D|11={}|55={}|54={}|38={}|40={}|49={}",
            order.orderId,
            order.instrumentId,
            if (order.side == Side.BUY) "1" else "2",
            order.quantity.toPlainString(),
            order.orderType,
            session.sessionId,
        )
    }
}
