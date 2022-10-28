package com.avars.server.p2p.server

import com.avars.server.EnvConfig
import com.avars.server.core.P2P_REMOVE_CLIENT
import com.avars.common.p2p.NetworkData
import com.avars.common.p2p.message.CHAT_MESSAGE
import com.avars.common.p2p.message.PING
import com.avars.common.p2p.packet.SOCKET_SIZE
import com.avars.common.p2p.packet.reader.BasicPacketReader
import com.avars.common.p2p.packet.reader.CryptoPacketReader
import com.avars.common.p2p.packet.reader.MessagePacketReader
import com.avars.common.p2p.packet.reader.PacketReader
import com.avars.server.core.QUERY_USER_BY_SESSION
import com.avars.server.p2p.server.handler.BaseHandler
import com.avars.server.p2p.server.handler.ChatInfoReqHandler
import com.avars.server.p2p.server.handler.PingReqHandler
import com.avars.server.storage.db.model.User
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class P2PServer(
    private val vertx: Vertx,
    private val coroutineContext: CoroutineContext
) {
    private val logger = LoggerFactory.getLogger(P2PServer::class.java)

    private var mPacketReader: PacketReader

    private val mHandlerMap: Map<Byte, BaseHandler<*>> = mapOf(
        PING to PingReqHandler(vertx),
        CHAT_MESSAGE to ChatInfoReqHandler(vertx)
    )

    init {
        val netServerOptions = NetServerOptions().apply {
            sendBufferSize = SOCKET_SIZE
            receiveBufferSize = SOCKET_SIZE
        }
        vertx
            .createNetServer(netServerOptions)
            .connectHandler(this::handleServerConnect)
            .listen(EnvConfig.tcpPort)

        val queryFunc: (suspend (String) -> String?) = { session ->
            vertx.eventBus().request<User?>(QUERY_USER_BY_SESSION, session).await().body()?.secret
        }
        mPacketReader = BasicPacketReader(CryptoPacketReader(MessagePacketReader(), queryFunc))
    }

    private fun handleServerConnect(socket: NetSocket) {
        logger.info("Connection from ${socket.remoteAddress()}")
        socket
            .handler { handleBuffer(socket, it) }
            .exceptionHandler { handleException(socket, it) }
            .closeHandler {
                logger.info("Connection from ${socket.remoteAddress()} closed")
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleBuffer(socket: NetSocket, buffer: Buffer) {
        GlobalScope.launch(coroutineContext) {
            mPacketReader.process(buffer.toString())?.let {
                mHandlerMap[it.type]?.execute(it, socket)
            }
        }
    }

    private fun handleException(socket: NetSocket, t: Throwable) {
        val remoteAddress = socket.remoteAddress()
        logger.warn("Connection from $remoteAddress exception, t: ${t.message}")
        val networkData = NetworkData(
            ip = remoteAddress.host(),
            port = remoteAddress.port()
        )
        vertx.eventBus().publish(P2P_REMOVE_CLIENT, networkData)
    }
}