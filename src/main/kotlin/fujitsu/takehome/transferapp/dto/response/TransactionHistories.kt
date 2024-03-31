package fujitsu.takehome.transferapp.dto.response

data class TransactionHistories(
    val transactions: List<TransactionHistory>,
    val hasMore: Boolean
)
