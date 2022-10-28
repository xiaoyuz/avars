package com.avars.common.p2p.packet.reader

import com.avars.common.p2p.message.P2PMessage
import com.avars.common.p2p.packet.P2PPacket

class BasicPacketReader(
    successor: PacketReader? = null
) : PacketReader(successor) {

    private var tempPacket: P2PPacket? = null

    override suspend fun process(data: String): P2PMessage? {
        val packets = fromString(data)
        packets.forEach { packet ->
            if (packet.withHead && packet.withTail) {
                return successorProcess(packet.content)
            } else {
                tempPacket = tempPacket?.concat(packet) ?: packet
                if (tempPacket!!.withHead && tempPacket!!.withTail) {
                    return successorProcess(tempPacket!!.content).also {
                        tempPacket = null
                    }
                }
            }
        }
        return null
    }

    private fun fromString(str: String): List<P2PPacket> {
        val packets = mutableListOf<P2PPacket>()
        var sb: StringBuilder? = null
        var packet: P2PPacket? = null
        str.forEach {
            if (packet == null) packet = P2PPacket()
            if (sb == null) sb = StringBuilder()
            if (it == '<') {
                sb = StringBuilder()
                packet!!.withHead = true
            } else if (it == '>') {
                packet!!.withTail = true
                packet!!.content = sb.toString()
                packets.add(packet!!)
                packet = null
                sb = null
            } else {
                sb!!.append(it)
            }
        }
        if (packet != null) {
            packet!!.content = sb.toString()
            packets.add(packet!!)
        }
        return packets
    }
}