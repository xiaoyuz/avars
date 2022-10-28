package com.avars.client.web

import com.avars.client.EnvConfig
import com.avars.client.core.HTTP_CREATE_SESSION
import com.avars.client.core.P2P_SEND
import com.avars.client.storage.*
import com.avars.common.UUID
import com.avars.common.account.Account
import com.avars.common.bean.DeviceInfoRequest
import com.avars.common.bean.DeviceInfoResponse
import com.avars.common.crypto.DiffieHellman
import com.avars.common.exception.Errors
import com.avars.common.exception.KeyException
import com.avars.common.genPairKey
import com.avars.common.p2p.message.ChatInfoMessage
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.redis.client.RedisAPI
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base64

class WebVerticle : CoroutineVerticle() {

    private lateinit var mRedisAPI: RedisAPI

    override suspend fun start() {
        val router = Router.router(vertx)
        val httpPort = config.getInteger("http_port")
        val redisHost = config.getString("redis_host")

        mRedisAPI = createRedisApi(vertx, redisHost)

        EnvConfig.tcpPort = config.getInteger("tcp_port")
        EnvConfig.deviceId = generateDeviceId()

        router.get("/hello").handler { handleCtx(it) {
            it.response().end("HelloClient")
        } }

        router.get("/device/register").handler { handleCtx(it) { registerDevice(it) } }
        router.get("/device/create_session").handler { handleCtx(it) { createSession(it) } }
        router.get("/device/testchat").handler { handleCtx(it) { testChat(it) } }

        vertx.createHttpServer().requestHandler(router).listen(httpPort).await()
    }

    private suspend fun generateDeviceId(): String {
        return mRedisAPI.get("$CLIENT_DEVICE_ID${EnvConfig.tcpPort}").await()?.toString() ?: UUID().apply {
            mRedisAPI.set(listOf("$CLIENT_DEVICE_ID${EnvConfig.tcpPort}", this))
        }
    }

    private suspend fun registerDevice(ctx: RoutingContext) {
        val deviceId = EnvConfig.deviceId
        val res = mRedisAPI.get("$CLIENT_DEVICE_ACCOUNT$deviceId").await()?.toString()?.let {
            Json.decodeValue(it, Account::class.java)
        }
        val account = res ?: Account(genPairKey()).apply {
            mRedisAPI.setnx("$CLIENT_DEVICE_ACCOUNT$deviceId", Json.encode(this))
        }
        ctx.response().end(Json.encode(account))
    }

    private suspend fun createSession(ctx: RoutingContext) {
        val deviceId = EnvConfig.deviceId
        mRedisAPI.get("$CLIENT_DEVICE_ACCOUNT$deviceId").await()?.toString()?.let {
            val account = Json.decodeValue(it, Account::class.java)
            val dh = DiffieHellman()
            val dhPub = dh.publicKeyStr()!!
            val content = UUID()
            val address = account.address
            val publicKey = account.keyPair.publicKey
            val sign = account.signData(content)
            val request = DeviceInfoRequest(
                device_id = deviceId,
                content = content,
                public_key = publicKey,
                address = address,
                sign = sign,
                dh_pub = dhPub
            )
            vertx.eventBus().request<DeviceInfoResponse?>(HTTP_CREATE_SESSION, request).await().body()?.let { response ->
                val session = response.session
                val serverDhPub = response.dh_pub
                dh.computeSecret(serverDhPub)
                val secretStr = Base64.encodeBase64String(dh.secret!!)
                mRedisAPI.setex("$CLIENT_SESSION${EnvConfig.tcpPort}", SESSION_EXPIRE_SECONDS, session).await()
                mRedisAPI.setex("$CLIENT_SECRET${EnvConfig.tcpPort}", SESSION_EXPIRE_SECONDS, secretStr).await()
                ctx.response().end((session to secretStr).toString())
            } ?: throw KeyException(Errors.INVALID_KEY_PAIR)
        }?: throw KeyException(Errors.INVALID_DEVICE_ID)
    }

    private suspend fun testChat(ctx: RoutingContext) {
        val deviceId = EnvConfig.deviceId
        val toAddress = ctx.queryParam("to").first()
        mRedisAPI.get("$CLIENT_DEVICE_ACCOUNT$deviceId").await()?.toString()?.let { accountStr ->
            val account = Json.decodeValue(accountStr, Account::class.java)
            val chatInfoMessage = ChatInfoMessage(
                fromAddress = account.address,
                toAddress = toAddress,
                content = "hello hello"
            )
            vertx.eventBus().request<String>(P2P_SEND, chatInfoMessage).await()
        }
        ctx.response().end()
    }

    private fun handleCtx(ctx: RoutingContext, func: suspend (ctx: RoutingContext) -> Unit) {
        launch {
            try {
                func(ctx)
            } catch (e: Exception) {
                ctx.response().end("Server error: ${e.message}")
            }
        }
    }
}