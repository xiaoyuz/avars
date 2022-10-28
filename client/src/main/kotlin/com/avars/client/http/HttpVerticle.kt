package com.avars.client.http

import com.avars.client.core.HTTP_CREATE_SESSION
import com.avars.common.bean.DeviceInfoRequest
import com.avars.common.bean.DeviceInfoResponse
import com.avars.common.core.HTTP_CODE_REQUEST_ERROR
import com.avars.common.handleMessage
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class HttpVerticle : CoroutineVerticle() {

    private lateinit var webClient: WebClient
    private lateinit var serverHost: String
    private var serverPort: Int = 0

    override suspend fun start() {
        serverHost = config.getString("server_host")
        serverPort = config.getInteger("server_port")
        webClient = WebClient.create(vertx)

        val bus = vertx.eventBus()
        bus.consumer(HTTP_CREATE_SESSION, this::createSession)
    }

    // Reply type is 'DeviceInfoResponse?'
    private fun createSession(message: Message<DeviceInfoRequest>) {
        handleMessage(message, HTTP_CODE_REQUEST_ERROR) {
            val deviceInfoRequest = message.body()
            val response = webClient
                .post(serverPort, serverHost, "/device/create_session")
                .sendJson(deviceInfoRequest).await().bodyAsJsonObject()
            if (response.getInteger("code") == 0) {
                val deviceInfoResponse = response.getJsonObject("content").mapTo(DeviceInfoResponse::class.java)
                message.reply(deviceInfoResponse)
            } else {
                message.reply(null)
            }
        }
    }
}