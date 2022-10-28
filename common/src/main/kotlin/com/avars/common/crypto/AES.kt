package com.avars.common.crypto

import org.apache.commons.codec.binary.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object AES {

    fun encode(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    fun decode(key: ByteArray, encryptedText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(encryptedText)
    }

    fun encode(key: String, data: String): String {
        val keyBytes = Base64.decodeBase64(key)
        val dataBytes = Base64.decodeBase64(data)
        val resBytes = encode(keyBytes, dataBytes)
        return Base64.encodeBase64String(resBytes)
    }

    fun decode(key: String, encryptedText: String): String {
        val keyBytes = Base64.decodeBase64(key)
        val encryptedTextBytes = Base64.decodeBase64(encryptedText)
        val resBytes = decode(keyBytes, encryptedTextBytes)
        return Base64.encodeBase64String(resBytes)
    }
}