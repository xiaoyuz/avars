package com.avars.client.p2p.client

import com.avars.client.EnvConfig
import com.avars.client.storage.CLIENT_DEVICE_ACCOUNT
import com.avars.client.storage.CLIENT_DEVICE_ID
import com.avars.client.storage.CLIENT_SECRET
import com.avars.client.storage.CLIENT_SESSION
import com.avars.common.account.Account
import com.avars.common.exception.Errors
import com.avars.common.exception.KeyException
import com.avars.common.getIp
import com.avars.common.p2p.NetworkData
import com.avars.common.p2p.message.P2PMessage
import com.avars.common.p2p.message.PING
import com.avars.common.p2p.message.PingMessage
import com.avars.common.p2p.packet.SOCKET_SIZE
import com.avars.common.p2p.packet.writer.BasicPacketWriter
import com.avars.common.p2p.packet.writer.CryptoPacketWriter
import com.avars.common.p2p.packet.writer.MessagePacketWriter
import com.avars.common.p2p.packet.writer.PacketWriter
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import io.vertx.redis.client.RedisAPI

class P2PClient(
    private val vertx: Vertx,
    private var redisAPI: RedisAPI
) {
    private val logger = LoggerFactory.getLogger(P2PClient::class.java)

    private val mNetClient: NetClient
    private var mConnectedSocket: NetSocket? = null

    private val packetWriter: PacketWriter by lazy { MessagePacketWriter(CryptoPacketWriter(BasicPacketWriter())) }

    init {
        val netClientOptions = NetClientOptions().apply {
            sendBufferSize = SOCKET_SIZE
            receiveBufferSize = SOCKET_SIZE
        }
        mNetClient = vertx.createNetClient(netClientOptions)
    }

    private suspend fun connect() {
        try {
            mConnectedSocket = mNetClient.connect(EnvConfig.p2pServerPort, EnvConfig.p2pServerHost).await()
            mConnectedSocket!!.exceptionHandler {
                mConnectedSocket = null
            }.closeHandler {
                logger.info("Connection closed to server")
                mConnectedSocket = null
            }
            logger.info("Connected to server")
        } catch (e: Exception) {
            logger.error("Connection failed to server")
        }
    }

    suspend fun ping() {
        redisAPI.get("$CLIENT_DEVICE_ACCOUNT${EnvConfig.deviceId}").await()?.toString()?.let {
            val account = Json.decodeValue(it, Account::class.java)
            if (mConnectedSocket == null) {
                connect()
            }
            val pingMessage = PingMessage(
                address = account.address,
                deviceId = EnvConfig.deviceId,
                networkData = NetworkData(
                    ip = getIp(),
                    port = EnvConfig.tcpPort
                )
            )
            send(
                P2PMessage(
                    type = PING,
                    data = Json.encode(pingMessage)
                )
            )
        }
    }

    suspend fun send(p2PMessage: P2PMessage) {
        val json = Json.encode(p2PMessage)
        val session = redisAPI.get("$CLIENT_SESSION${EnvConfig.tcpPort}").await()?.toString() ?: return
        val secret = redisAPI.get("$CLIENT_SECRET${EnvConfig.tcpPort}").await()?.toString() ?: return
        packetWriter.process(json, session, secret)?.let {
            mConnectedSocket?.write(it)
        }
    }
}