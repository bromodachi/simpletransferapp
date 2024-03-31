package fujitsu.takehome.transferapp.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

data class TransferRequestDto(
    val fromAccount: Long,
    val toAccount: Long,
    @field:Min(1)
    val amount: Long,
    @field:NotEmpty
    val uniqueId: String,
    val currency: String,
)