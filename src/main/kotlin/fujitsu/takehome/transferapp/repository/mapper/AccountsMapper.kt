package fujitsu.takehome.transferapp.repository.mapper

import fujitsu.takehome.transferapp.dto.AccountTransfer
import fujitsu.takehome.transferapp.entity.Account
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.springframework.stereotype.Component

@Mapper
@Component
interface AccountsMapper {
    fun getAccountByAccountId(@Param("account_id") accountId: Long): Account?

    fun updateAccountBalance(
        @Param("account_transfer") accountTransfer: AccountTransfer,
    ): Int

    fun insertAccount(
        @Param("account") account: Account
    ): Int
}