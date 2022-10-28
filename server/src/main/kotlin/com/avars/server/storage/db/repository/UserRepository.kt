package com.avars.server.storage.db.repository

import com.avars.server.storage.db.model.User
import com.avars.server.storage.db.model.convertUser
import com.avars.server.storage.db.query
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.Tuple

class UserRepository(private val mySQLPool: MySQLPool) {

    suspend fun findByAddress(address: String): User? {
        val queryStr = "SELECT * FROM user WHERE address = ?"
        val tuple = Tuple.of(address)
        return query(mySQLPool, queryStr, tuple).map { convertUser(it) }.firstOrNull()
    }

    suspend fun findByDeviceId(deviceId: String): User? {
        val queryStr = "SELECT * FROM user WHERE device_id = ?"
        val tuple = Tuple.of(deviceId)
        return query(mySQLPool, queryStr, tuple).map { convertUser(it) }.firstOrNull()
    }

    suspend fun findBySession(session: String): User? {
        val queryStr = "SELECT * FROM user WHERE session = ?"
        val tuple = Tuple.of(session)
        return query(mySQLPool, queryStr, tuple).map { convertUser(it) }.firstOrNull()
    }

    suspend fun insertOrUpdate(user: User) {
        try {
            val queryStr = "INSERT INTO user(address, device_id, session, secret) VALUES (?, ?, ?, ?)"
            val tuple = Tuple.of(user.address, user.deviceId, user.session, user.secret)
            query(mySQLPool, queryStr, tuple)
        } catch (e: Exception) {
            val queryStr = "UPDATE user SET device_id = ?, session = ?, secret = ? WHERE address = ?"
            val tuple = Tuple.of(user.deviceId, user.session, user.secret, user.address)
            query(mySQLPool, queryStr, tuple)
        }
    }
}