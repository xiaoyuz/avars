package com.avars.client.storage

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions

suspend fun createRedisApi(vertx: Vertx, redisHost: String): RedisAPI {
    val connection = Redis.createClient(
        vertx,
        RedisOptions().apply {
            setConnectionString("redis://$redisHost")
        }
    ).connect().await()
    return RedisAPI.api(connection)
}