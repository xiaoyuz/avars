package com.avars.common.bean

data class DeviceInfoResponse(
    var session: String = "",
    var dh_pub: String = ""
) : java.io.Serializable