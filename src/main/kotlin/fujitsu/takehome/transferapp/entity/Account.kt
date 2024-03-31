package fujitsu.takehome.transferapp.entity

import fujitsu.takehome.transferapp.annotations.NoArgsConstructor

@NoArgsConstructor
data class Account(
    val id: Long = 0,
    val userId: Long,
    val balance: Long,
    val currency: String
)