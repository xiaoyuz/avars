package com.avars.server.storage.db.model

import io.vertx.sqlclient.Row

fun convertUser(row: Row) = User(
    id = row.getLong("id"),
    address = row.getString("address"),
    deviceId = row.getString("device_id"),
    session = row.getString("session"),
    secret = row.getString("secret")
)