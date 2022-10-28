package com.avars.server.web

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch

class WebVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val router = Router.router(vertx)
        val httpPort = config.getInteger("http_port")

        router.get("/hello").handler { handleCtx(it) { it.response().end("Hello33444") } }

        router.post("/device/create_session")
            .handler(BodyHandler.create()).handler { handleCtx(it) { createSession(it) } }

        vertx.createHttpServer().requestHandler(router).listen(httpPort).await()
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