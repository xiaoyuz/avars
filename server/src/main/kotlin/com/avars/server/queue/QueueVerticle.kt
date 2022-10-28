package com.avars.server.queue

import com.avars.common.core.MESSAGE_QUEUE_CODE_ERROR
import com.avars.common.handleMessage
import com.avars.common.p2p.message.ChatInfoMessage
import com.avars.server.core.MESSAGE_QUEUE_ADD
import com.avars.server.core.MESSAGE_QUEUE_AQUIRE
import com.avars.server.queue.db.DBStore
import com.avars.server.queue.db.KEY_MESSAGE_QUEUE_ADDRESS
import com.avars.server.queue.db.LevelDBStore
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.commons.codec.binary.Base64
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File

private const val STORE_SPLITER = ">"

class QueueVerticle : CoroutineVerticle() {

    private lateinit var mLevelDB: DB
    private lateinit var mDBStore: DBStore

    override suspend fun start() {
        val levelPath = config.getString("level_path")

        val options = Options().apply { createIfMissing(true) }
        try {
            mLevelDB = Iq80DBFactory.factory.open(File(levelPath), options)
            mDBStore = LevelDBStore(mLevelDB)

            val bus = vertx.eventBus()
            bus.consumer(MESSAGE_QUEUE_ADD, this::addQueue)
            bus.consumer(MESSAGE_QUEUE_AQUIRE, this::aquireQueue)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Reply type is 'String'
    private fun addQueue(message: Message<ChatInfoMessage>) {
        handleMessage(message, MESSAGE_QUEUE_CODE_ERROR) { mes ->
            val chatInfoMessage = mes.body()
            val key = "$KEY_MESSAGE_QUEUE_ADDRESS${chatInfoMessage.toAddress}"
            val value = Base64.encodeBase64String(Json.encode(chatInfoMessage).toByteArray())
            val storedValue = mDBStore.get(key)?.let {
                "$it$STORE_SPLITER$value"
            } ?: value
            mDBStore.put(key, storedValue)
            mes.reply("")
        }
    }

    // Reply type is 'List<ChatInfoMessage>'
    private fun aquireQueue(message: Message<String>) {
        handleMessage(message, MESSAGE_QUEUE_CODE_ERROR) { mes ->
            val toAddress = mes.body()
            val key = "$KEY_MESSAGE_QUEUE_ADDRESS$toAddress}"
            val chatInfoMessages = mDBStore.get(key)?.split(STORE_SPLITER)?.map {
                Json.decodeValue(String(Base64.decodeBase64(it)), ChatInfoMessage::class.java)
            } ?: emptyList()
            mes.reply(chatInfoMessages)
        }
    }
}