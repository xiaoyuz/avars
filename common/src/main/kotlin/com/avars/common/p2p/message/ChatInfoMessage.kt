package com.avars.common.p2p.message

const val TEXT_TYPE: Byte = 0

data class ChatInfoMessage(
    var commonInfo: CommonInfo = CommonInfo(),
    var fromAddress: String = "",
    var toAddress: String = "",
    var infoType: Byte = TEXT_TYPE,
    var content: String = ""
) : java.io.Serializable
