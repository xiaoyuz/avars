package com.avars.server.core

const val REDIS_SET_SESSION = "redis.set.session"
const val REDIS_GET_SESSION = "redis.get.session"

const val CREATE_NEW_SESSION = "create.new.session"
const val QUERY_USER_BY_SESSION = "query.user.by.session"
const val QUERY_USER_BY_ADDRESS = "query.user.by.address"

const val P2P_PING = "p2p.ping"
const val P2P_REMOVE_CLIENT = "p2p.remove.client"
const val P2P_SOCKET_EXISTED = "p2p.socket.existed"
const val P2P_CHAT_MESSAGES_SEND = "p2p.chat.messages.send"
const val P2P_CHAT_MESSAGE_ARRIVE = "p2p.chat.message.arrive"
const val P2P_CHAT_CLIENT_CONNECT = "p2p.chat.client.connect"