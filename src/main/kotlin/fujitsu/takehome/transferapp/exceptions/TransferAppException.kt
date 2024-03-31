package fujitsu.takehome.transferapp.exceptions

import fujitsu.takehome.transferapp.constants.ApiErrorCodes
import org.springframework.http.HttpStatus


sealed class TransferAppException(
    open val status: HttpStatus,
    open val apiErrorCodes: ApiErrorCodes,
    override val message: String = "AN UNKNOWN ERROR OCCURRED"
): RuntimeException(message)

class BadRequestException(
    override val status: HttpStatus = HttpStatus.BAD_REQUEST,
    override val apiErrorCodes: ApiErrorCodes = ApiErrorCodes.INVALID_REQUEST,
    override val message: String = "Bad request"
) : TransferAppException(status, apiErrorCodes, message)

class InternalServerException(
    override val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    override val apiErrorCodes: ApiErrorCodes = ApiErrorCodes.INTERNAL_SERVER_EXCEPTION,
    override val message: String = "Internal server error"
): TransferAppException(status, apiErrorCodes, message)

class DuplicateException(
    override val status: HttpStatus = HttpStatus.BAD_REQUEST,
    override val apiErrorCodes: ApiErrorCodes = ApiErrorCodes.DUPLICATE_REQUEST,
    override val message: String = "DUPLICATE REQUEST RECEIVED"
): TransferAppException(status, apiErrorCodes, message)

class NotFoundException(
    override val status: HttpStatus = HttpStatus.BAD_REQUEST,
    override val apiErrorCodes: ApiErrorCodes = ApiErrorCodes.NOT_FOUND,
    override val message: String = "NOT FOUND"
): TransferAppException(status, apiErrorCodes, message)

class DatabaseErrorException(
    override val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    override val apiErrorCodes: ApiErrorCodes = ApiErrorCodes.DATABASE_ERROR,
    override val message: String = "Database error"
): TransferAppException(status, apiErrorCodes, message)