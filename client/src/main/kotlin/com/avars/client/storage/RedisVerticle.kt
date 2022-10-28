package com.avars.client.storage

import com.avars.client.core.REDIS_GET_DEVICE_ACCOUNT
import com.avars.client.core.REDIS_SET_DEVICE_ACCOUNT
import com.avars.common.account.Account
import com.avars.common.core.STORAGE_CODE_REDIS_ERROR
import com.avars.common.handleMessage
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
        bus.consumer(REDIS_GET_DEVICE_ACCOUNT, this::getDeviceAccount)
        bus.consumer(REDIS_SET_DEVICE_ACCOUNT, this::setDeviceAccount)
    }

    // Reply type is 'Account?'
    private fun getDeviceAccount(message: Message<String>) {
        handleMessage(message, STORAGE_CODE_REDIS_ERROR) { mes ->
            val deviceId = mes.body()
            mRedisAPI.get("$CLIENT_DEVICE_ACCOUNT$deviceId").await()?.toString()?.let {
                mes.reply(Json.decodeValue(it, Account::class.java))
            } ?: mes.reply(null)
        }
    }

    // Reply type is 'String'
    private fun setDeviceAccount(message: Message<Pair<String, Account>>) {
        handleMessage(message, STORAGE_CODE_REDIS_ERROR) { mes ->
            val (deviceId, account) = mes.body()
            mRedisAPI.setnx("$CLIENT_DEVICE_ACCOUNT$deviceId", Json.encode(account))
            mes.reply("")
        }
    }
}