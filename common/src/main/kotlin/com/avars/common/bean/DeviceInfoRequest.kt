package com.avars.common.bean

import com.avars.common.verify

data class DeviceInfoRequest(
    var device_id: String = "",
    var content: String = "",
    var public_key: String = "",
    var address: String = "",
    var sign: String = "",
    var dh_pub: String = ""
) : java.io.Serializable {

    fun verifyContent() = verify(content, sign, public_key)
}