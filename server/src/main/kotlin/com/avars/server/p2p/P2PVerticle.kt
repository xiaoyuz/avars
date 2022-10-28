package com.avars.server.p2p

import com.avars.common.core.P2P_CODE_ERROR
import com.avars.common.handleMessage
import com.avars.common.p2p.NetworkData
import com.avars.common.p2p.message.ChatInfoMessage
import com.avars.server.EnvConfig
import com.avars.server.core.P2P_PING
import com.avars.server.core.P2P_REMOVE_CLIENT
import com.avars.server.p2p.client.P2PClient
import com.avars.common.p2p.message.PingMessage
import com.avars.server.core.P2P_CHAT_MESSAGE_SEND
import com.avars.server.p2p.server.P2PServer
import io.vertx.core.eventbus.Message
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle

class P2PVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(P2PVerticle::class.java)

    private lateinit var p2pClient: P2PClient
    private lateinit var p2pServer: P2PServer

    override suspend fun start() {
        EnvConfig.tcpPort = config.getInteger("tcp_port")

        p2pClient = P2PClient(vertx)
        p2pServer = P2PServer(vertx, coroutineContext)

        val bus = vertx.eventBus()
        bus.consumer(P2P_PING, this::ping)
        bus.consumer(P2P_REMOVE_CLIENT, this::removeClient)
        bus.consumer(P2P_CHAT_MESSAGE_SEND, this::chatMessageSend)
    }

    private fun ping(message: Message<PingMessage>) {
        handleMessage(message, P2P_CODE_ERROR) {
            val pingMessage = it.body()
            if (!p2pClient.socketExisted(pingMessage.address)) {
                p2pClient.addClient(pingMessage.address, pingMessage.networkData)
            }
            message.reply("")
        }
    }

    private fun removeClient(message: Message<NetworkData>) {
        handleMessage(message, P2P_CODE_ERROR) {
            p2pClient.closeClient(it.body())
            message.reply("")
        }
    }

    private fun chatMessageSend(message: Message<ChatInfoMessage>) {
        handleMessage(message, P2P_CODE_ERROR) {
            p2pClient.send(it.body())
            message.reply("")
        }
    }
}