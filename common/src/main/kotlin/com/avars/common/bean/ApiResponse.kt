package com.avars.common.bean

data class ApiResponse<T>(var code: Int = 0,
                          var message: String = "",
                          var content: T? = null) : java.io.Serializable {

    companion object {
        fun success() = ApiResponse<Any>()

        fun failed(code: Int, msg: String) = ApiResponse<Unit>(code, msg)

        fun <T> fail(code: Int, msg: String) = ApiResponse<T>(code, msg)

        fun <T> success(content: T?): ApiResponse<T> {
            return ApiResponse(content = content)
        }

        fun <T> failed(content: T?, code: Int, message: String) = ApiResponse(code, message, content)
    }
}