package com.avars.common.crypto

import java.security.MessageDigest

fun ByteArray.toHex() = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

object BaseAlgo {

    fun encode(algo: String, data: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance(algo)
        messageDigest.update(data)
        return messageDigest.digest()
    }

    fun encodeStr(algo: String, data: String) = encode(algo, data.toByteArray())

    fun encodeTwice(algo: String, data: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance(algo)
        messageDigest.update(data)
        return messageDigest.digest(messageDigest.digest())
    }
}