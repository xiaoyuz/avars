package com.avars.common

import com.avars.common.crypto.Base58
import com.avars.common.crypto.ECDSA
import com.avars.common.crypto.SHA256
import com.avars.common.exception.Errors
import com.avars.common.exception.KeyException
import org.apache.commons.codec.binary.Base64
import java.security.MessageDigest

fun genPairKey() = genPairKey(false)

fun genPairKey(encodePubKey: Boolean) = try {
    ECDSA.genPrivateKey().let { priv ->
        KeyPair(ECDSA.genPublicKey(priv, encodePubKey), priv)
    }
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun checkPairKey(privateKey: String, publicKey: String) = try {
    publicKey.trim() == ECDSA.genPublicKey(privateKey.trim(), false)
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun genPublicKey(privateKey: String, encode: Boolean) = try {
    ECDSA.genPublicKey(privateKey, encode)
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun genPublicKey(privateKey: String) = genPublicKey(privateKey, false)

fun genAddressByPub(publicKey: String): String {
    try {
        val hashSha256 = SHA256.sha256(Base64.decodeBase64(publicKey))
        val messageDigest = MessageDigest.getInstance("RipeMD160")
        messageDigest.update(hashSha256)
        val hashRipeMD160 = messageDigest.digest()
        val hashTwiceSha256 = SHA256.sha256Twice(hashRipeMD160)
        val rawAddr = ByteArray(1 + hashRipeMD160.size + 4)
        rawAddr[0] = 0
        System.arraycopy(hashRipeMD160, 0, rawAddr, 1, hashRipeMD160.size)
        System.arraycopy(hashTwiceSha256, 0, rawAddr, hashRipeMD160.size + 1, 4)
        return Base58.encode(rawAddr)
    } catch (e: Exception) {
        throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
    }
}

fun genAddressByPriv(privateKey: String) = try {
    genAddressByPub(ECDSA.genPublicKey(privateKey))
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun sign(privateKey: String, data: String) =try {
    ECDSA.sign(privateKey, data)
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun sign(privateKey: String, data: ByteArray) = try {
    ECDSA.sign(privateKey, data)
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun verify(src: String, sign: String, publicKey: String) = try {
    ECDSA.verify(src, sign, publicKey)
} catch (e: Exception) {
    throw KeyException(Errors.ECDSA_ENCRYPT_ERROR, e)
}

fun main() {
    val kp = genPairKey()
    println(kp.privateKey)
}