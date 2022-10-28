package com.avars.common.p2p.message

const val PING: Byte = 0
const val CHAT_MESSAGE: Byte = 1

data class P2PMessage(
    var type: Byte = 0,
    var data: String = ""
) : java.io.Serializable