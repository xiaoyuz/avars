package com.avars.common.p2p.packet.reader

import com.avars.common.p2p.message.P2PMessage

abstract class PacketReader(
    private val successor: PacketReader? = null
) {
    abstract suspend fun process(data: String): P2PMessage?

    suspend fun successorProcess(data: String) = successor?.process(data)
}