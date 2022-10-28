package com.avars.common.p2p.packet.writer

class BasicPacketWriter(
    successor: PacketWriter? = null
) : PacketWriter(successor) {

    override fun process(data: String, vararg params: String): String {
        return "<$data>"
    }
}