package com.avars.common.crypto

import java.math.BigInteger
import java.util.*

object Base58 {
    private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
    private val ENCODED_ZERO = ALPHABET[0]
    private val INDEXES = IntArray(128)

    init {
        Arrays.fill(INDEXES, -1)
        for (i in ALPHABET.indices) {
            INDEXES[ALPHABET[i].code] = i
        }
    }

    /**
     * Encodes the given bytes as a base58 string (no checksum is appended).
     *
     * @param input
     * the bytes to encode
     * @return the base58-encoded string
     */
    fun encode(inputByteArray: ByteArray): String {
        var input = inputByteArray
        if (input.isEmpty()) {
            return ""
        }
        // Count leading zeros.
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) {
            ++zeros
        }
        // Convert base-256 digits to base-58 digits (plus conversion to ASCII
        // characters)
        input = input.copyOf(input.size) // since we modify it
        // in-place
        val encoded = CharArray(input.size * 2) // upper bound
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < input.size) {
            encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58).toInt()]
            if (input[inputStart].toInt() == 0) {
                ++inputStart // optimization - skip leading zeros
            }
        }
        // Preserve exactly as many leading encoded zeros in output as there
        // were leading zeros in input.
        while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
            ++outputStart
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ENCODED_ZERO
        }
        // Return encoded string (including encoded leading zeros).
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    /**
     * Decodes the given base58 string into the original data bytes.
     *
     * @param input
     * the base58-encoded string to decode
     * @return the decoded data bytes
     * @throws AddressFormatException
     * if the given string is not a valid base58 string
     */
    @Throws(RuntimeException::class)
    fun decode(input: String): ByteArray {
        if (input.isEmpty()) {
            return ByteArray(0)
        }
        // Convert the base58-encoded ASCII chars to a base58 byte sequence
        // (base58 digits).
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            if (digit < 0) {
                throw RuntimeException("Illegal character $c at position $i")
            }
            input58[i] = digit.toByte()
        }
        // Count leading zeros.
        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) {
            ++zeros
        }
        // Convert base-58 digits to base-256 digits.
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256)
            if (input58[inputStart].toInt() == 0) {
                ++inputStart // optimization - skip leading zeros
            }
        }
        // Ignore extra leading zeroes that were added during the calculation.
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
            ++outputStart
        }
        // Return decoded data (including original number of leading zeros).
        return decoded.copyOfRange(outputStart - zeros, decoded.size)
    }

    @Throws(RuntimeException::class)
    fun decodeToBigInteger(input: String): BigInteger {
        return BigInteger(1, decode(input))
    }
    /**
     * Decodes the given base58 string into the original data bytes, using the
     * checksum in the last 4 bytes of the decoded data to verify that the rest
     * are correct. The checksum is removed from the returned data.
     *
     * @param input
     * the base58-encoded string to decode (which should include the
     * checksum)
     * @throws AddressFormatException
     * if the input is not base 58 or the checksum does not
     * validate.
     *
     * public static byte[] decodeChecked(String input) throws
     * AddressFormatException { byte[] decoded = decode(input); if
     * (decoded.length < 4) throw new
     * AddressFormatException("Input too short"); byte[] data =
     * Arrays.copyOfRange(decoded, 0, decoded.length - 4); byte[]
     * checksum = Arrays.copyOfRange(decoded, decoded.length - 4,
     * decoded.length); byte[] actualChecksum =
     * Arrays.copyOfRange(Sha256Hash.hashTwice(data), 0, 4); if
     * (!Arrays.equals(checksum, actualChecksum)) throw new
     * AddressFormatException("Checksum does not validate"); return
     * data; }
     */
    /**
     * Divides a number, represented as an array of bytes each containing a
     * single digit in the specified base, by the given divisor. The given
     * number is modified in-place to contain the quotient, and the return value
     * is the remainder.
     *
     * @param number
     * the number to divide
     * @param firstDigit
     * the index within the array of the first non-zero digit (this
     * is used for optimization by skipping the leading zeros)
     * @param base
     * the base in which the number's digits are represented (up to
     * 256)
     * @param divisor
     * the number to divide by (up to 256)
     * @return the remainder of the division operation
     */
    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
        // this is just long division which accounts for the base of the input
        // digits
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}