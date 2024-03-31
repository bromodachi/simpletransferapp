package fujitsu.takehome.transferapp.entity

import fujitsu.takehome.transferapp.annotations.NoArgsConstructor

@NoArgsConstructor
data class Transaction(
    // will only be zero when inserting.
    val id: Long = 0,
    val uniqueId: String,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Long,
    val currency: String,
    // will only be zero when inserting.
    val createdAt: Long = 0
)