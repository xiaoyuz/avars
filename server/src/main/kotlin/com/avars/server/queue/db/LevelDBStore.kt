package com.avars.server.queue.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.iq80.leveldb.DB

class LevelDBStore(private val levelDB: DB) : DBStore {

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.Default) {
        levelDB.put(key.toByteArray(), value.toByteArray())
    }

    override suspend fun get(key: String) = withContext(Dispatchers.Default) {
        levelDB.get(key.toByteArray())?.let { String(it) }
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        levelDB.delete(key.toByteArray())
    }
}