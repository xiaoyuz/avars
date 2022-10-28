package com.avars.common.p2p.message

import com.avars.common.p2p.NetworkData

data class PingMessage(
    var address: String = "",
    var deviceId: String = "",
    var networkData: NetworkData = NetworkData()
) : java.io.Serializable