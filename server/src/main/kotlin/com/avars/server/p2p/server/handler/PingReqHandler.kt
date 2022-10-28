package com.avars.server.p2p.server.handler

import com.avars.server.core.P2P_PING
import com.avars.common.p2p.message.PingMessage
import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket

class PingReqHandler(vertx: Vertx) : BaseHandler<PingMessage>(vertx) {

    override fun dataClass() = PingMessage::class.java

    override suspend fun handle(data: PingMessage, type: Byte, socket: NetSocket) {
        vertx.eventBus().request<String>(P2P_PING, data)
    }
}