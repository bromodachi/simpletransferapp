package fujitsu.takehome.transferapp.dto.response

data class TransactionHistory(
    val id: Long,
    val accountId: Long,
    val amount: String,
    val createdAt: Long,
    val currency: String
)