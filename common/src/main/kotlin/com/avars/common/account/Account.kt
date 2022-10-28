package com.avars.common.account

import com.avars.common.*

data class Account(
    val keyPair: KeyPair = KeyPair(),
    var address: String = ""
) : java.io.Serializable {

    init {
        if (keyPair.privateKey != "") address = genAddressByPriv(keyPair.privateKey)
    }

    constructor(
        privateKey: String
    ) : this(
        keyPair = KeyPair(privateKey = privateKey, publicKey = genPublicKey(privateKey))
    )

    fun checkKeyPair() = checkPairKey(keyPair.privateKey, keyPair.publicKey)

    fun signData(data: String) = sign(keyPair.privateKey, data)

    fun verifyData(src: String, sign: String) = verify(src, sign, keyPair.publicKey)
}