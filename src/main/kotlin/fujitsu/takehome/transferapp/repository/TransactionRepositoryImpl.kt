package fujitsu.takehome.transferapp.repository

import fujitsu.takehome.transferapp.dto.AccountTransfer
import fujitsu.takehome.transferapp.dto.request.TransferRequestDto
import fujitsu.takehome.transferapp.entity.Transaction
import fujitsu.takehome.transferapp.repository.mapper.TransactionMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.ibatis.annotations.Param
import org.springframework.stereotype.Repository

@Repository
class TransactionRepositoryImpl(
    private val transactionMapper: TransactionMapper
): TransactionRepository {

    override fun createTransaction(request: TransferRequestDto) {
        try {
            transactionMapper.createTransaction(
                Transaction(
                    fromAccountId = request.fromAccount,
                    toAccountId = request.toAccount,
                    amount = request.amount,
                    uniqueId = request.uniqueId,
                    currency = request.currency,
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Unknown exception thrown when trying to create an exception" }
            throw e
        }
    }

    override fun getTransactionByIdempotencyKey(uniqueId: String): Transaction? = transactionMapper.getTransactionByIdempotencyKey(uniqueId)

    override fun getAccountTransactionHistories(accountId: Long, limit: Int, lastIdSeen: Long?): List<Transaction> {
        return transactionMapper.getAccountTransactionHistories(accountId, limit, lastIdSeen)
    }

    companion object {
        private val logger = KotlinLogging.logger {  }
    }
}