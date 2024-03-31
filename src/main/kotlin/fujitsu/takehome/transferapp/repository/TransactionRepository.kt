package fujitsu.takehome.transferapp.repository

import fujitsu.takehome.transferapp.dto.request.TransferRequestDto
import fujitsu.takehome.transferapp.entity.Transaction

interface TransactionRepository {
    /**
     * Creates a transaction between two accounts
     */
    fun createTransaction(request: TransferRequestDto)

    fun getTransactionByIdempotencyKey(uniqueId: String): Transaction?

    /**
     * Retrieves all transfers for a user.
     */
    fun getAccountTransactionHistories(accountId: Long, limit: Int, lastIdSeen: Long?): List<Transaction>
}