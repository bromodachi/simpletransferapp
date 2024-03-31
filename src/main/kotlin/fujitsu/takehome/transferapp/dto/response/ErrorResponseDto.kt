package fujitsu.takehome.transferapp.dto.response

import java.util.*

data class ErrorResponseDto(
    val timestamp: Date,
    val errorCode: String,
    val message: String,
    val description: String
)