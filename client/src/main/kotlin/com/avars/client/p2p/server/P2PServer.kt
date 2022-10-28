package com.avars.client.p2p.server

import com.avars.client.EnvConfig
import com.avars.client.storage.CLIENT_SECRET
import com.avars.common.p2p.packet.SOCKET_SIZE
import com.avars.common.p2p.packet.reader.BasicPacketReader
import com.avars.common.p2p.packet.reader.CryptoPacketReader
import com.avars.common.p2p.packet.reader.MessagePacketReader
import com.avars.common.p2p.packet.reader.PacketReader
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import io.vertx.redis.client.RedisAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class P2PServer(
    private val vertx: Vertx,
    private var redisAPI: RedisAPI,
    private val coroutineContext: CoroutineContext
) {

    private val logger = LoggerFactory.getLogger(P2PServer::class.java)

    private var mPacketReader: PacketReader

    init {
        val netServerOptions = NetServerOptions().apply {
            sendBufferSize = SOCKET_SIZE
            receiveBufferSize = SOCKET_SIZE
        }
        vertx
            .createNetServer(netServerOptions)
            .connectHandler(this::handleServerConnect)
            .listen(EnvConfig.tcpPort)

        val queryFunc: (suspend (String) -> String?) = {
            redisAPI.get("$CLIENT_SECRET${EnvConfig.tcpPort}").await().toString()
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

    private fun handleBuffer(socket: NetSocket, buffer: Buffer) {
        GlobalScope.launch(coroutineContext) {
            mPacketReader.process(buffer.toString())?.let {
                logger.info("Receive chat message: $it")
            }
        }
    }

    private fun handleException(socket: NetSocket, t: Throwable) {
        val remoteAddress = socket.remoteAddress()
        logger.warn("Connection from $remoteAddress exception, t: ${t.message}")
    }
}