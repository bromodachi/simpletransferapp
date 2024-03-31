package fujitsu.takehome.transferapp.service

import fujitsu.takehome.transferapp.dto.request.TransferRequestDto
import fujitsu.takehome.transferapp.dto.response.AccountDetails
import fujitsu.takehome.transferapp.dto.response.TransactionHistories
import fujitsu.takehome.transferapp.dto.response.TransactionHistory

interface AccountService {
    /**
     * Get account details for an id. If account is not found, an exception will be thrown
     */
    fun getAccountDetails(id: Long): AccountDetails

    fun checkDuplicateTransactionRequest(request: TransferRequestDto): Boolean

    /**
     * Get all transaction history for an account.
     */
    fun getAccountTransactionHistories(
        id: Long,
        lastIdSeen: Long?,
        limit: Int
    ): TransactionHistories

    /**
     * Transfer between two accounts
     */
    fun transferBetweenAccounts(request: TransferRequestDto)
}