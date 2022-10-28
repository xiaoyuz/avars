package com.avars.server.queue.db

interface DBStore {

    suspend fun put(key: String, value: String)

    suspend fun get(key: String): String?

    suspend fun remove(key: String)
}