package com.avars.server

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val envConfigJson = loadEnvConfig(vertx)
        val configJson = Vertx.currentContext().config()

        deployVerticles(vertx, configJson)
    }
}

// For debugging
suspend fun main() {
    val vertx = Vertx.vertx()
    val configJson = loadFileConfig(vertx)
    deployVerticles(vertx, configJson)
}

private suspend fun loadEnvConfig(vertx: Vertx): JsonObject {
    val envRetriever = ConfigRetriever.create(vertx,
        ConfigRetrieverOptions()
            .addStore(
                ConfigStoreOptions()
                    .setType("env")
                    .setConfig(
                        JsonObject().put("raw-data", true)
                    )
            )
    )
    return envRetriever.config.await()
}

private suspend fun loadFileConfig(vertx: Vertx): JsonObject {
    val retriever = ConfigRetriever.create(vertx,
        ConfigRetrieverOptions().addStore(
            ConfigStoreOptions().setType("file").setConfig(
                JsonObject().put("path", "conf/config.json")
            )
        )
    )
    return retriever.config.await()
}

private suspend fun deployVerticles(vertx: Vertx, config: JsonObject) {
    vertx.deployVerticle(
        "com.avars.server.web.WebVerticle", DeploymentOptions().setConfig(config)
    ).await()
    vertx.deployVerticle(
        "com.avars.server.storage.db.DBVerticle", DeploymentOptions().setConfig(config)
    ).await()
    vertx.deployVerticle(
        "com.avars.server.p2p.P2PVerticle", DeploymentOptions().setConfig(config)
    ).await()
    vertx.deployVerticle(
        "com.avars.server.queue.QueueVerticle", DeploymentOptions().setConfig(config)
    ).await()
}
