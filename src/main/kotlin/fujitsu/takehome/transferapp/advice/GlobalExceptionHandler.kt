package fujitsu.takehome.transferapp.advice

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import fujitsu.takehome.transferapp.constants.ApiErrorCodes
import fujitsu.takehome.transferapp.dto.response.ErrorResponseDto
import fujitsu.takehome.transferapp.exceptions.TransferAppException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.*

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}
    private fun createErrorResponseDto(apiErrorCodes: ApiErrorCodes, description: String): ErrorResponseDto {
        return ErrorResponseDto(
            timestamp = Date(),
            errorCode = apiErrorCodes.errorCode,
            message = apiErrorCodes.name,
            description = description
        )
    }

    @ExceptionHandler(Exception::class)
    fun generalException(e: Exception): ResponseEntity<Any> {
        logger.error(e) { "Unknown exception" }
        return ResponseEntity(
            createErrorResponseDto(
                ApiErrorCodes.INTERNAL_SERVER_EXCEPTION,
                e.message ?: "INTERNAL_SERVER_ERROR"
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun invalidJson(e: HttpMessageNotReadableException): ResponseEntity<Any> {
        val cause = e.cause
        if (cause is MissingKotlinParameterException) {
            val message = handleMissingKotlinParameterViolation(cause)
            return ResponseEntity(
                createErrorResponseDto(
                    ApiErrorCodes.INVALID_REQUEST,
                    message
                ),
                HttpStatus.BAD_REQUEST
            )
        }
        return ResponseEntity(
            createErrorResponseDto(
                ApiErrorCodes.INVALID_REQUEST,
                e.message ?: "INTERNAL_SERVER_ERROR"
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    private fun handleMissingKotlinParameterViolation(cause: MissingKotlinParameterException): String {
        return cause.path.joinToString {
            it.fieldName + " must not be null or must not be empty"
        }
    }

    /**
     * This error occurs when a validation fails in the dto's validation.
     * @param exception MethodArgumentNotValidException: the error to use to get necessary information.
     * @return ResponseEntity with out custom response object
     * @throws IOException
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<Any> {
        val bindingResult = exception.bindingResult
        var message: String? = null
        for (fieldError in bindingResult.fieldErrors) {
            message = handleFieldErrors(fieldError, message)
        }
        return ResponseEntity(
            createErrorResponseDto(ApiErrorCodes.INVALID_REQUEST, message ?: "INVALID_REQUEST"),
            HttpStatus.BAD_REQUEST
        )
    }

    private fun handleFieldErrors(fieldError: FieldError, start: String?): String {
        val sb = StringBuilder()
        val builder: StringBuilder = start?.let { s -> sb.append(s).append(", ") } ?: sb
        builder.append("field: ").append(fieldError.field).append(". ").append("error: ").append(fieldError.defaultMessage)
        return builder.toString()
    }

    @ExceptionHandler(TransferAppException::class)
    fun handleRuntimeExceptions(exception: TransferAppException): ResponseEntity<Any> {
        if (exception.status.is5xxServerError) {
            logger.error(exception) { "server exception was thrown" }
        }
        return ResponseEntity(
            createErrorResponseDto(
                exception.apiErrorCodes,
                exception.message
            ),
            exception.status
        )
    }
}