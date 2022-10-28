package com.avars.common.crypto

object SHA256 {
    fun sha256(str: String) = BaseAlgo.encodeStr("SHA-256", str)

    fun sha256(data: ByteArray) = BaseAlgo.encode("SHA-256", data)

    fun sha256Twice(data: ByteArray) = BaseAlgo.encodeTwice("SHA-256", data)
}