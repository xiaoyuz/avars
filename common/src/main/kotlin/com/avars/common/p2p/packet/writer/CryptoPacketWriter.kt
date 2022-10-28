package com.avars.common.p2p.packet.writer

import com.avars.common.crypto.AES
import com.avars.common.p2p.packet.PacketContent
import io.vertx.core.json.Json
import org.apache.commons.codec.binary.Base64

class CryptoPacketWriter(
    successor: PacketWriter? = null,
) : PacketWriter(successor) {

    override fun process(data: String, vararg params: String): String? {
        if (params.size < 2) return null
        val (session, secret) = params
        val encryptedData = AES.encode(secret, data)
        val packetContent = PacketContent(
            data = encryptedData,
            session = session
        )
        val json = Json.encode(packetContent)
        return successorProcess(Base64.encodeBase64String(json.toByteArray()), *params)
    }
}