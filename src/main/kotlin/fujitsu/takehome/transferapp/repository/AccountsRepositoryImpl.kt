package fujitsu.takehome.transferapp.repository

import fujitsu.takehome.transferapp.constants.USD
import fujitsu.takehome.transferapp.dto.AccountTransfer
import fujitsu.takehome.transferapp.dto.response.AccountDetails
import fujitsu.takehome.transferapp.repository.mapper.AccountsMapper
import fujitsu.takehome.transferapp.utils.CurrencyUtil
import org.springframework.stereotype.Repository

@Repository
class AccountsRepositoryImpl(
    private val accountsMapper: AccountsMapper
): AccountsRepository {
    override fun getAccountDetails(id: Long): AccountDetails? {
        return accountsMapper.getAccountByAccountId(accountId = id)
            ?.let { AccountDetails(it.id, it.userId, CurrencyUtil.formatAmount(it.balance, USD).toString(), it.currency) }
    }

    override fun updateAccountBalance(accountTransfer: AccountTransfer): Boolean {
        return accountsMapper.updateAccountBalance(accountTransfer) > 0
    }
}