package com.avars.server.p2p.server.handler

import com.avars.common.p2p.message.P2PMessage
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.net.NetSocket

abstract class BaseHandler<T>(
    var vertx: Vertx
) {

    protected val logger = LoggerFactory.getLogger(this::class.java)

    abstract fun dataClass(): Class<T>

    suspend fun execute(p2PMessage: P2PMessage, socket: NetSocket) {
        val dataStr = p2PMessage.data
        val data = Json.decodeValue(dataStr, dataClass())
        handle(data, p2PMessage.type, socket)
    }

    abstract suspend fun handle(data: T, type: Byte, socket: NetSocket)
}