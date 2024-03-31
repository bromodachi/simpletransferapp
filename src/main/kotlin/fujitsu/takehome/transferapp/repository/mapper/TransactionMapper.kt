package fujitsu.takehome.transferapp.repository.mapper

import fujitsu.takehome.transferapp.entity.Transaction
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.springframework.stereotype.Component

@Mapper
@Component
interface TransactionMapper {
    fun createTransaction(@Param("transaction") transaction: Transaction)
    fun getTransactionByIdempotencyKey(@Param("unique_id")uniqueId: String): Transaction?
    fun getAccountTransactionHistories(
        @Param("account_id") accountId: Long,
        @Param("limit") limit: Int,
        @Param("last_id_seen")  lastIdSeen: Long? = null
    ): List<Transaction>
}