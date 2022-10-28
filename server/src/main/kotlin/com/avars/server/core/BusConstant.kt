package com.avars.server.core

const val REDIS_SET_SESSION = "redis.set.session"
const val REDIS_GET_SESSION = "redis.get.session"

const val CREATE_NEW_SESSION = "create.new.session"
const val QUERY_USER_BY_SESSION = "query.user.by.session"
const val QUERY_USER_BY_ADDRESS = "query.user.by.address"

const val P2P_PING = "p2p.ping"
const val P2P_REMOVE_CLIENT = "p2p.remove.client"
const val P2P_CHAT_MESSAGE_SEND = "p2p.chat.message.send"

const val MESSAGE_QUEUE_ADD = "message.queue.add"
const val MESSAGE_QUEUE_AQUIRE = "message.queue.aquire"