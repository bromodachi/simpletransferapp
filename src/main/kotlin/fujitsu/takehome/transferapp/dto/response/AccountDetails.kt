package fujitsu.takehome.transferapp.dto.response

data class AccountDetails(
    val id: Long,
    val userId: Long,
    val balance: String,
    val currency: String
)
