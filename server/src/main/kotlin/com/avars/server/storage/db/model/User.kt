package com.avars.server.storage.db.model

data class User(
    var id: Long = 0,
    var address: String = "",
    var deviceId: String = "",
    var session: String = "",
    var secret: String = ""
) : java.io.Serializable