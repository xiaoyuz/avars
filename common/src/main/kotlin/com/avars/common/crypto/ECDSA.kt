package com.avars.common.crypto

import org.apache.commons.codec.binary.Base64
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.RandomDSAKCalculator
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.FixedPointUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom

const val RANDOM_NUMBER_ALGORITHM = "SHA1PRNG"
const val RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN"

object ECDSA {

    private var CURVE: ECDomainParameters
    private var HALF_CURVE_ORDER: BigInteger

    private val maxPrivKey = BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16)

    init {
        val curveParam = CustomNamedCurves.getByName("secp256k1")
        FixedPointUtil.precompute(curveParam.g)
        CURVE = ECDomainParameters(
            curveParam.curve, curveParam.g, curveParam.n,
            curveParam.h
        )
        HALF_CURVE_ORDER = curveParam.n.shiftRight(1)
    }

    fun genPrivateKey(): String {
        val secureRandom = try {
            SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER)
        } catch (e: Exception) {
            SecureRandom()
        }
        // Generate the key, skipping as many as desired.
        // Generate the key, skipping as many as desired.
        val privateKeyAttempt = ByteArray(32)
        secureRandom.nextBytes(privateKeyAttempt)
        var privateKeyCheck = BigInteger(1, privateKeyAttempt)
        while (privateKeyCheck.compareTo(BigInteger.ZERO) == 0 || privateKeyCheck > maxPrivKey) {
            secureRandom.nextBytes(privateKeyAttempt)
            privateKeyCheck = BigInteger(1, privateKeyAttempt)
        }
        val result = Base64.encodeBase64String(privateKeyAttempt)
        return result.replace("[\\s*\t\n\r]".toRegex(), "")
    }

    fun genPublicKey(privateKeyStr: String, encode: Boolean): String {
        val privateKeyBytes = Base64.decodeBase64(privateKeyStr)
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val pointQ = spec.g.multiply(BigInteger(1, privateKeyBytes))
        val result = Base64.encodeBase64String(pointQ.getEncoded(encode))
        return result.replace(Regex("[\\s*\t\n\r]"), "")
    }

    fun genPublicKey(privateKeyStr: String) = genPublicKey(privateKeyStr, false)

//    fun getAddress(publicKey: String): String {
//
//    }
//
//    fun getAddress(keyBytes: ByteArray): String {
//
//    }

    fun sign(privateKey: String, data: String) = sign(privateKey, data.toByteArray())

    fun sign(privateKey: String, data: ByteArray): String {
        val hash256 = SHA256.sha256(data)
        // Here we use random cal to generate different signer each time
        val signer = ECDSASigner(RandomDSAKCalculator())
        val pri = BigInteger(1, Base64.decodeBase64(privateKey))
        val privKey = ECPrivateKeyParameters(pri, CURVE)
        signer.init(true, privKey)
        val components = signer.generateSignature(hash256)
        val content: ByteArray = ECDSASignature(components[0], components[1]).toCanonicalised().encodeToDER()
        val result = Base64.encodeBase64String(content)
        return result.replace("[\\s*\t\n\r]".toRegex(), "")
    }

    fun verify(src: String, sign: String, publicKey: String): Boolean {
        val hash256 = SHA256.sha256(src)
        val eCDSASignature = decodeFromDER(Base64.decodeBase64(sign))
        val signer = ECDSASigner()
        val pub = CURVE.curve.decodePoint(Base64.decodeBase64(publicKey))
        val params = ECPublicKeyParameters(
            CURVE.curve.decodePoint(pub.getEncoded(false)),
            CURVE
        )
        signer.init(false, params)
        return signer.verifySignature(hash256, eCDSASignature.r, eCDSASignature.s)
    }

    private fun decodeFromDER(bytes: ByteArray): ECDSASignature {
        var decoder: ASN1InputStream? = null
        return try {
            decoder = ASN1InputStream(bytes)
            val seq: DLSequence = decoder.readObject() as DLSequence
                ?: throw RuntimeException("Reached past end of ASN.1 stream.")
            val r: ASN1Integer
            val s: ASN1Integer
            try {
                r = seq.getObjectAt(0) as ASN1Integer
                s = seq.getObjectAt(1) as ASN1Integer
            } catch (e: ClassCastException) {
                throw IllegalArgumentException(e)
            }
            // OpenSSL deviates from the DER spec by interpreting these
            // values as unsigned, though they should not be
            // Thus, we always use the positive versions. See:
            // http://r6.ca/blog/20111119T211504Z.html
            ECDSASignature(r.positiveValue, s.positiveValue)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            decoder?.close()
        }
    }

    data class ECDSASignature(
        var r: BigInteger,
        var s: BigInteger,
    ) {
        /**
         * Returns true if the S component is "low", that means it is below
         * See [* BIP62](https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures).
         */
        fun isCanonical() = s <= HALF_CURVE_ORDER

        /**
         * Will automatically adjust the S component to be less than or equal to
         * half the curve order, if necessary. This is required because for
         * every signature (r,s) the signature (r, -s (mod N)) is a valid
         * signature of the same message. However, we dislike the ability to
         * modify the bits of a Bitcoin transaction after it's been signed, as
         * that violates various assumed invariants. Thus in future only one of
         * those forms will be considered legal and the other will be banned.
         */
        fun toCanonicalised() = if (!isCanonical()) {
            // The order of the curve is the number of valid points that
            // exist on that curve. If S is in the upper
            // half of the number of valid points, then bring it back to the
            // lower half. Otherwise, imagine that
            // N = 10
            // s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are
            // valid solutions.
            // 10 - 8 == 2, giving us always the latter solution, which is
            // canonical.
            ECDSASignature(r, CURVE.n.subtract(s))
        } else this

        /**
         * DER is an international standard for serializing data structures
         * which is widely used in cryptography. It's somewhat like protocol
         * buffers but less convenient. This method returns a standard DER
         * encoding of the signature, as recognized by OpenSSL and other
         * libraries.
         */
        fun encodeToDER() = try {
            derByteStream().toByteArray()
        } catch (e: IOException) {
            // Cannot happen.
            throw RuntimeException(e)
        }

        fun derByteStream(): ByteArrayOutputStream {
            // Usually 70-72 bytes.
            val bos = ByteArrayOutputStream(72)
            val seq = DERSequenceGenerator(bos)
            seq.addObject(ASN1Integer(r))
            seq.addObject(ASN1Integer(s))
            seq.close()
            return bos
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || javaClass != o.javaClass) {
                return false
            }
            val other = o as ECDSASignature
            return r == other.r && s == other.s
        }

        override fun hashCode(): Int {
            return arrayOf(r, s).contentHashCode()
        }
    }
}

fun main() {
    val priKey = ECDSA.genPrivateKey()
    val pubKey = ECDSA.genPublicKey(priKey)
    println(priKey)
    println(pubKey)
    val data = "hjehehflkasdfaksdjf"
    val sign1 = ECDSA.sign(priKey, data)
    val sign2 = ECDSA.sign(priKey, data)
    println(sign1)
    println(sign2)
    val verify1 = ECDSA.verify(data, sign1, pubKey)
    val verify2 = ECDSA.verify(data, sign2, pubKey)
    println(verify1)
    println(verify2)
}