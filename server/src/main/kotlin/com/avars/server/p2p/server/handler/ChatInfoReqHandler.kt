package com.avars.server.p2p.server.handler

import com.avars.common.p2p.message.ChatInfoMessage
import com.avars.server.core.P2P_CHAT_MESSAGE_ARRIVE
import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket

class ChatInfoReqHandler(vertx: Vertx) : BaseHandler<ChatInfoMessage>(vertx) {

    override fun dataClass() = ChatInfoMessage::class.java

    override suspend fun handle(data: ChatInfoMessage, type: Byte, socket: NetSocket) {
        vertx.eventBus().publish(P2P_CHAT_MESSAGE_ARRIVE, data)
    }
}