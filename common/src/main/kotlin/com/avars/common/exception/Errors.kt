package com.avars.common.exception

enum class Errors(
    var code: String,
    var message: String
) {
    INVALID_PARAM_ERROR("101", "invalid param error"),
    AES_ENCRYPT_ERROR("103", "AES encrypt error"),
    ECDSA_ENCRYPT_ERROR("104", "ECDSA encrypt error"),
    SIGN_ERROR("105", "sign error"),
    GENERATE_SIGN_ERROR("106", "generate sign error"),
    GENERATE_SQL_ERROR("107", "generate sql error"),
    VERIFY_SIGN_ERROR("108", "verify sign error"),
    VERIFY_HASH_ERROR("109", "verify hash error"),
    INVALID_DH_ERROR("110", "invalid dh key"),

    INVALID_KEY_PAIR("301", "invalid key pair"),
    INVALID_CONTENT("302", "invalid content"),

    INVALID_DEVICE_ID("401", "invalid device id"),
    INVALID_SESSION("402", "invalid session"),
}