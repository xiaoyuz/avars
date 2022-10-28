package com.avars.common.exception

data class KeyException(
    var code: String,
    var msg: String
) : Exception(msg) {

    constructor(rtnCd: String, rtnMsg: String, t: Throwable) : this(rtnCd, rtnMsg) {
        initCause(t)
    }

    constructor(errors: Errors) : this(errors.code, errors.message)

    constructor(errors: Errors, t: Throwable) : this(errors.code, errors.message) {
        initCause(t)
    }
}