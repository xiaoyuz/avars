package com.avars.common.p2p

import com.avars.common.getAddress

data class NetworkData(
    var ip: String = "",
    var port: Int = 0
): java.io.Serializable {

    fun pingAddress() = getAddress(ip, port)
}