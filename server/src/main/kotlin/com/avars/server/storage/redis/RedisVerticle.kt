package com.avars.server.storage.redis

import com.avars.common.core.STORAGE_CODE_REDIS_ERROR
import com.avars.common.handleMessage
import com.avars.server.core.REDIS_GET_SESSION
import com.avars.server.core.REDIS_SET_SESSION
import com.avars.server.core.SessionInfo
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.redis.client.RedisAPI

class RedisVerticle : CoroutineVerticle() {

    private lateinit var mRedisAPI: RedisAPI

    override suspend fun start() {
        val redisHost = config.getString("redis_host")

        mRedisAPI = createRedisApi(vertx, redisHost)

        val bus = vertx.eventBus()
        bus.consumer(REDIS_GET_SESSION, this::getSession)
        bus.consumer(REDIS_SET_SESSION, this::setSession)
    }

    // Reply type is 'SessionInfo?'
    private fun getSession(message: Message<String>) {
        handleMessage(message, STORAGE_CODE_REDIS_ERROR) { mes ->
            val session = mes.body()
            mRedisAPI.get("$SESSION_INFO$session").await()?.toString()?.let {
                mes.reply(Json.decodeValue(it, SessionInfo::class.java))
            } ?: mes.reply(null)
        }
    }

    // Reply type is 'String'
    private fun setSession(message: Message<Pair<String, SessionInfo>>) {
        handleMessage(message, STORAGE_CODE_REDIS_ERROR) { mes ->
            val (session, sessionInfo) = mes.body()
            mRedisAPI.setex("$SESSION_INFO$session", SESSION_EXPIRE_SECONDS,
                Json.encode(sessionInfo)).await()
            mes.reply("")
        }
    }
}