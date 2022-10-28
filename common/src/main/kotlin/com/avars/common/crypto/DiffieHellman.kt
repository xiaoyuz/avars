package com.avars.common.crypto

import org.apache.commons.codec.binary.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

private const val ECDH_KEY = "ECDH"
private const val EC_KEY = "EC"

class DiffieHellman(
    var publicKey: PublicKey? = null,
    var secret: ByteArray?= null
) {

    private var keyAgreement: KeyAgreement

    init {
        val kpg = KeyPairGenerator.getInstance(EC_KEY)
        kpg.initialize(128)
        val kp = kpg.generateKeyPair()
        publicKey = kp.public
        keyAgreement = KeyAgreement.getInstance(ECDH_KEY)
        keyAgreement.init(kp.private)
    }

    fun computeSecret(peerPublicKey: PublicKey) {
        keyAgreement.doPhase(peerPublicKey, true)
        secret = keyAgreement.generateSecret()
    }

    fun computeSecret(peerPublicKeyStr: String) {
        keyAgreement.doPhase(string2PublicKey(peerPublicKeyStr), true)
        secret = keyAgreement.generateSecret()
    }

    fun publicKeyStr() = publicKey?.let { publicKey2String(it) }
}

private fun string2PublicKey(str: String): PublicKey {
    val bytes = Base64.decodeBase64(str)
    val factory = KeyFactory.getInstance(EC_KEY)
    return factory.generatePublic(X509EncodedKeySpec(bytes))
}

private fun publicKey2String(publicKey: PublicKey): String {
    val bytes = publicKey.encoded
    return Base64.encodeBase64String(bytes)
}

// Test
fun main() {
    val d1 = DiffieHellman()
    val d2 = DiffieHellman()

    d1.computeSecret(d2.publicKey!!)

    val d1PubStr = publicKey2String(d1.publicKey!!)
    println(d1PubStr)

    val doubleStr = publicKey2String(string2PublicKey(d1PubStr))
    println(doubleStr)

    println(Base64.encodeBase64String(d1.secret!!))

    d2.computeSecret(d1PubStr)
    println(Base64.encodeBase64String(d2.secret!!))
}