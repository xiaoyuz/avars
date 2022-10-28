package com.avars.common.p2p.packet.reader

import com.avars.common.crypto.AES
import com.avars.common.exception.Errors
import com.avars.common.exception.KeyException
import com.avars.common.p2p.message.P2PMessage
import com.avars.common.p2p.packet.PacketContent
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import org.apache.commons.codec.binary.Base64

class CryptoPacketReader(
    successor: PacketReader? = null,
    private val querySecret: suspend (String) -> String?
) : PacketReader(successor) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun process(data: String): P2PMessage? {
        val decodedData = String(Base64.decodeBase64(data))
        return try {
            val packetContent = Json.decodeValue(decodedData, PacketContent::class.java)
            val session = packetContent.session
            val secret = querySecret(session) ?: throw KeyException(Errors.INVALID_SESSION)
            val content = AES.decode(secret, packetContent.data)
            successorProcess(content)
        } catch (e: Exception) {
            logger.error(e)
            null
        }
    }
}