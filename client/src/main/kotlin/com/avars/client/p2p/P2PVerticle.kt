package com.avars.client.p2p

import com.avars.client.EnvConfig
import com.avars.client.core.P2P_SEND
import com.avars.client.p2p.client.P2PClient
import com.avars.client.p2p.server.P2PServer
import com.avars.client.storage.createRedisApi
import com.avars.common.core.P2P_CODE_ERROR
import com.avars.common.handleMessage
import com.avars.common.p2p.message.CHAT_MESSAGE
import com.avars.common.p2p.message.ChatInfoMessage
import com.avars.common.p2p.message.P2PMessage
import com.avars.common.p2p.message.PING
import io.vertx.core.eventbus.Message
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.redis.client.RedisAPI
import kotlinx.coroutines.launch

private const val SCHEDULE_PING_TIME = 5 * 1000L // 5s

class P2PVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(P2PVerticle::class.java)

    private lateinit var p2pClient: P2PClient
    private lateinit var p2pServer: P2PServer

    private lateinit var mRedisAPI: RedisAPI

    override suspend fun start() {
        EnvConfig.p2pServerHost = config.getString("p2p_server_host")
        EnvConfig.p2pServerPort = config.getInteger("p2p_server_port")
        val redisHost = config.getString("redis_host")

        mRedisAPI = createRedisApi(vertx, redisHost)

        p2pClient = P2PClient(vertx, mRedisAPI)
        p2pServer = P2PServer(vertx, mRedisAPI, coroutineContext)

        val bus = vertx.eventBus()
        bus.consumer(P2P_SEND, this::send)

        vertx.setTimer(SCHEDULE_PING_TIME, this::pingServer)
    }

    private fun pingServer(timerId: Long) {
        logger.info("Ping server")
        launch { p2pClient.ping() }
        vertx.setTimer(SCHEDULE_PING_TIME, this::pingServer)
    }

    private fun send(message: Message<ChatInfoMessage>) {
        logger.info("Send message: ${message.body()}")
        handleMessage(message, P2P_CODE_ERROR) {
            val p2PMessage = P2PMessage(
                type = CHAT_MESSAGE,
                data = Json.encode(it.body())
            )
            p2pClient.send(p2PMessage)
            message.reply("")
        }
    }
}