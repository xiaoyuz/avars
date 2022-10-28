package com.avars.server.storage.db

import com.avars.common.core.STORAGE_CODE_DB_ERROR
import com.avars.common.handleMessage
import com.avars.server.core.CREATE_NEW_SESSION
import com.avars.server.core.QUERY_USER_BY_ADDRESS
import com.avars.server.core.QUERY_USER_BY_SESSION
import com.avars.server.storage.db.model.User
import com.avars.server.storage.db.repository.UserRepository
import com.avars.server.core.SessionInfo
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions

class DBVerticle : CoroutineVerticle() {

    private lateinit var mMysqlPool: MySQLPool

    private lateinit var mUserRepository: UserRepository

    override suspend fun start() {
        val mysqlConnectionOptions = MySQLConnectOptions().apply {
            host = config.getString("mysql_host")
            port = config.getInteger("mysql_port")
            database = config.getString("mysql_database")
            user = config.getString("mysql_user")
            password = config.getString("mysql_password")
        }
        val poolOptions = PoolOptions().apply { maxSize = 10 }
        mMysqlPool = MySQLPool.pool(vertx, mysqlConnectionOptions, poolOptions)

        mUserRepository = UserRepository(mMysqlPool)

        val bus = vertx.eventBus()
        bus.consumer(CREATE_NEW_SESSION, this::createNewSession)
        bus.consumer(QUERY_USER_BY_SESSION, this::queryUserBySession)
        bus.consumer(QUERY_USER_BY_ADDRESS, this::queryUserByAddress)
    }

    // Reply type is 'String'
    private fun createNewSession(message: Message<Pair<String, SessionInfo>>) {
        handleMessage(message, STORAGE_CODE_DB_ERROR) { mes ->
            val (session, info) = mes.body()
            val user = User(
                address = info.address,
                deviceId = info.device_id,
                session = session,
                secret = info.secret
            )
            mUserRepository.insertOrUpdate(user)
            mes.reply("")
        }
    }

    // Reply type is 'User?'
    private fun queryUserBySession(message: Message<String>) {
        handleMessage(message, STORAGE_CODE_DB_ERROR) { mes ->
            val session = mes.body()
            val user = mUserRepository.findBySession(session)
            mes.reply(user)
        }
    }

    // Reply type is 'User?'
    private fun queryUserByAddress(message: Message<String>) {
        handleMessage(message, STORAGE_CODE_DB_ERROR) { mes ->
            val address = mes.body()
            val user = mUserRepository.findByAddress(address)
            mes.reply(user)
        }
    }
}