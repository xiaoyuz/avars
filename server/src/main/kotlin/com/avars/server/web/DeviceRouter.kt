package com.avars.server.web

import com.avars.common.UUID
import com.avars.common.bean.ApiResponse
import com.avars.common.bean.DeviceInfoRequest
import com.avars.common.bean.DeviceInfoResponse
import com.avars.common.crypto.DiffieHellman
import com.avars.common.exception.Errors
import com.avars.common.exception.KeyException
import com.avars.server.core.CREATE_NEW_SESSION
import com.avars.server.core.REDIS_SET_SESSION
import com.avars.server.core.SessionInfo
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import org.apache.commons.codec.binary.Base64

fun createSession(ctx: RoutingContext) {
    val vertx = ctx.vertx()
    val deviceInfoRequest = ctx.body().asJsonObject().mapTo(DeviceInfoRequest::class.java)
    if (!deviceInfoRequest.verifyContent()) throw KeyException(Errors.VERIFY_SIGN_ERROR)
    val dh = DiffieHellman()
    val clientDhPub = deviceInfoRequest.dh_pub
    dh.computeSecret(clientDhPub)
    val serverDhPub = dh.publicKeyStr() ?: throw KeyException(Errors.INVALID_DH_ERROR)
    val secret = dh.secret ?: throw KeyException(Errors.INVALID_DH_ERROR)
    val secretStr = Base64.encodeBase64String(secret)
    val sessionInfo = SessionInfo(
        device_id = deviceInfoRequest.device_id,
        address = deviceInfoRequest.address,
        secret = secretStr
    )
    val session = UUID()
    vertx.eventBus().publish(CREATE_NEW_SESSION, session to sessionInfo)
    val deviceInfoResponse = DeviceInfoResponse(
        session = session,
        dh_pub = serverDhPub
    )
    val response = ApiResponse.success(deviceInfoResponse)
    ctx.response().end(Json.encode(response))
}