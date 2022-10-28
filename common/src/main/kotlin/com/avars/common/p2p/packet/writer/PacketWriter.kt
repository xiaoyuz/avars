package com.avars.common.p2p.packet.writer

abstract class PacketWriter(
    private val successor: PacketWriter? = null
) {
    abstract fun process(data: String, vararg params: String): String?

    fun successorProcess(data: String, vararg params: String) = successor?.process(data, *params)
}