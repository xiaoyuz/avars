package com.avars.server.core

data class SessionInfo(
    var device_id: String = "",
    var address: String = "",
    var secret: String = ""
) : java.io.Serializable