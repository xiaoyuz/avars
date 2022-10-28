package com.avars.server.storage.redis

const val SESSION_EXPIRE_SECONDS = (30 * 24 * 60 * 60).toString() // 30 Days

const val SESSION_INFO = "session_info:"