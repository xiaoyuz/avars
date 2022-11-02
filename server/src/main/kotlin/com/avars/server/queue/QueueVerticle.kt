package com.avars.server.queue

import com.avars.common.core.MESSAGE_QUEUE_CODE_ERROR
import com.avars.common.handleMessage
import com.avars.common.p2p.message.ChatInfoMessage
import com.avars.server.core.*
import com.avars.server.queue.db.DBStore
import com.avars.server.queue.db.KEY_MESSAGE_QUEUE_ADDRESS
import com.avars.server.queue.db.LevelDBStore
import io.vertx.core.eventbus.Message
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.apache.commons.codec.binary.Base64
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File

private const val STORE_SPLITER = ">"

class QueueVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(QueueVerticle::class.java)

    private lateinit var mLevelDB: DB
    private lateinit var mDBStore: DBStore

    override suspend fun start() {
        val levelPath = config.getString("level_path")

        val options = Options().apply { createIfMissing(true) }
        try {
            mLevelDB = Iq80DBFactory.factory.open(File(levelPath), options)
            mDBStore = LevelDBStore(mLevelDB)

            val bus = vertx.eventBus()
            bus.consumer(P2P_CHAT_CLIENT_CONNECT, this::clientConnected)
            bus.consumer(P2P_CHAT_MESSAGE_ARRIVE, this::chatMessageArrive)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Reply type is 'String'
    private fun chatMessageArrive(message: Message<ChatInfoMessage>) {
        handleMessage(message, MESSAGE_QUEUE_CODE_ERROR) { mes ->
            val chatInfoMessage = mes.body()
            val clientEnable = vertx.eventBus().request<Boolean>(P2P_SOCKET_EXISTED, chatInfoMessage.toAddress)
                .await().body()
            if (clientEnable) {
                vertx.eventBus().publish(P2P_CHAT_MESSAGES_SEND, listOf(chatInfoMessage))
                return@handleMessage
            }
            addQueue(chatInfoMessage)
        }
    }

    // Reply type is 'String'
    private fun clientConnected(message: Message<String>) {
        handleMessage(message, MESSAGE_QUEUE_CODE_ERROR) { mes ->
            logger.info("New connect to client, address is ${mes.body()}, checking message queue")
            val chatInfoMessages = aquireQueue(mes.body())
            logger.info("Message queue size: ${chatInfoMessages.size}")
            if (chatInfoMessages.isNotEmpty()) {
                vertx.eventBus().publish(P2P_CHAT_MESSAGES_SEND, chatInfoMessages)
                val key = "$KEY_MESSAGE_QUEUE_ADDRESS${mes.body()}"
                mDBStore.remove(key)
            }
            mes.reply("")
        }
    }

    private suspend fun addQueue(chatInfoMessage: ChatInfoMessage) {
        val key = "$KEY_MESSAGE_QUEUE_ADDRESS${chatInfoMessage.toAddress}"
        val value = Base64.encodeBase64String(Json.encode(chatInfoMessage).toByteArray())
        val storedValue = mDBStore.get(key)?.let {
            "$it$STORE_SPLITER$value"
        } ?: value
        mDBStore.put(key, storedValue)
    }

    private suspend fun aquireQueue(address: String): List<ChatInfoMessage> {
        val key = "$KEY_MESSAGE_QUEUE_ADDRESS$address"
        return mDBStore.get(key)?.split(STORE_SPLITER)?.map {
            Json.decodeValue(String(Base64.decodeBase64(it)), ChatInfoMessage::class.java)
        } ?: emptyList()
    }
}