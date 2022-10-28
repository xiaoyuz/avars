package com.avars.server.storage.db

import io.vertx.kotlin.coroutines.await
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

suspend fun query(mysqlPool: MySQLPool, queryStr: String, tuple: Tuple): RowSet<Row> {
    val connection = mysqlPool.connection.await()
    return connection
        .preparedQuery(queryStr)
        .execute(tuple).await().apply {
            connection.close()
        }
}