package fujitsu.takehome.transferapp.constants

enum class ApiErrorCodes(
    val errorCode: String,
) {
    UNKNOWN_ERROR(getErrorCode(0)),
    DUPLICATE_REQUEST(getErrorCode(1)),
    NOT_FOUND(getErrorCode(2)),
    INVALID_REQUEST(getErrorCode(3)),
    DATABASE_ERROR(getErrorCode(4)),
    INTERNAL_SERVER_EXCEPTION(getErrorCode(9999));

    companion object {
        const val serviceErrorCode = "911"
    }
}

private fun getErrorCode(value: Int): String {
    return ApiErrorCodes.serviceErrorCode + "$value".padStart(4, '4')
}