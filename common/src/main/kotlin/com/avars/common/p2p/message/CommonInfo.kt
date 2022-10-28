package com.avars.common.p2p.message

import com.avars.common.UUID

data class CommonInfo(
    var timeMs: Long = System.currentTimeMillis(),
    var requestId: String = UUID(),
    var responseId: String = "",
) : java.io.Serializable