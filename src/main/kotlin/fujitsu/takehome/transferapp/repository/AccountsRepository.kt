package fujitsu.takehome.transferapp.repository

import fujitsu.takehome.transferapp.dto.AccountTransfer
import fujitsu.takehome.transferapp.dto.response.AccountDetails

interface AccountsRepository {
    fun getAccountDetails(id: Long): AccountDetails?
    fun updateAccountBalance(accountTransfer: AccountTransfer): Boolean
}