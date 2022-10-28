package com.avars.server.p2p.client

import com.avars.common.p2p.NetworkData
import com.avars.common.p2p.message.ChatInfoMessage
import com.avars.common.p2p.message.P2PMessage
import com.avars.common.p2p.message.PING
import com.avars.common.p2p.packet.SOCKET_SIZE
import com.avars.common.p2p.packet.writer.BasicPacketWriter
import com.avars.common.p2p.packet.writer.CryptoPacketWriter
import com.avars.common.p2p.packet.writer.MessagePacketWriter
import com.avars.common.p2p.packet.writer.PacketWriter
import com.avars.server.core.QUERY_USER_BY_ADDRESS
import com.avars.server.core.QUERY_USER_BY_SESSION
import com.avars.server.storage.db.model.User
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await

var connectedSockets = mutableMapOf<String, NetSocket>()
var addressNetworkMap = mutableMapOf<String, NetworkData>()
var networkAddressMap = mutableMapOf<NetworkData, String>()

class P2PClient(
    private val vertx: Vertx
) {
    private val logger = LoggerFactory.getLogger(P2PClient::class.java)

    private val mNetClient: NetClient
    private val mPacketWriter: PacketWriter by lazy { MessagePacketWriter(CryptoPacketWriter(BasicPacketWriter())) }

    init {
        val netClientOptions = NetClientOptions().apply {
            sendBufferSize = SOCKET_SIZE
            receiveBufferSize = SOCKET_SIZE
        }
        mNetClient = vertx.createNetClient(netClientOptions)
    }

    fun socketExisted(address: String) = connectedSockets.containsKey(address)

    suspend fun closeClient(networkData: NetworkData) {
        networkAddressMap[networkData]?.let { address ->
            connectedSockets[address]?.close()?.await()
            removeConnected(address, networkData)
        }
    }

    suspend fun addClient(address: String, networkData: NetworkData) {
        try {
            val socket = mNetClient.connect(networkData.port, networkData.ip).await()
            socket
                .exceptionHandler {
                    removeConnected(address, networkData)
                }
                .closeHandler {
                    removeConnected(address, networkData)
                    logger.info("Connection closed to ${socket.remoteAddress()}, list: $connectedSockets")
                }
            addConnected(socket, address, networkData)
            logger.info("Connection to ${socket.remoteAddress()}, list: $connectedSockets")
        } catch (e: Exception) {
            logger.error("Connection failed: ${networkData.pingAddress()}")
        }
    }

    suspend fun send(chatInfoMessage: ChatInfoMessage) {
        val address = chatInfoMessage.toAddress
        connectedSockets[address]?.let { netSocket ->
            vertx.eventBus().request<User?>(QUERY_USER_BY_ADDRESS, address).await().body()?.let { toUser ->
                val message = P2PMessage(
                    type = PING,
                    data = Json.encode(chatInfoMessage)
                )
                mPacketWriter.process(Json.encode(message), toUser.session, toUser.secret)?.let {
                    netSocket.write(it)
                }
            }
        }




    }

    private fun addConnected(socket: NetSocket, address: String, networkData: NetworkData) {
        connectedSockets[address] = socket
        addressNetworkMap[address] = networkData
        networkAddressMap[networkData] = address
    }

    private fun removeConnected(address: String, networkData: NetworkData) {
        connectedSockets.remove(address)
        addressNetworkMap.remove(address)
        networkAddressMap.remove(networkData)
    }
}