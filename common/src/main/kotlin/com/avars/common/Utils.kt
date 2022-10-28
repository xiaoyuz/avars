package com.avars.common

import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.net.InetAddress

fun UUID() = java.util.UUID.randomUUID().toString().replace("\\-".toRegex(), "")

fun <T> CoroutineVerticle.handleMessage(
    message: Message<T>,
    errorCode: Int,
    func: suspend (message: Message<T>) -> Unit
) {
    launch {
        try {
            func(message)
        } catch (e: Exception) {
            message.fail(errorCode, e.message)
        }
    }
}

fun getAddress(ip: String, port: Int) = "$ip:$port"

fun getIp() = InetAddress.getLocalHost().hostAddress