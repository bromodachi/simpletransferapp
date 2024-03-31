package fujitsu.takehome.transferapp.service

import fujitsu.takehome.transferapp.dto.AccountTransfer
import fujitsu.takehome.transferapp.dto.request.TransferRequestDto
import fujitsu.takehome.transferapp.dto.response.AccountDetails
import fujitsu.takehome.transferapp.dto.response.TransactionHistories
import fujitsu.takehome.transferapp.dto.response.TransactionHistory
import fujitsu.takehome.transferapp.exceptions.BadRequestException
import fujitsu.takehome.transferapp.exceptions.DatabaseErrorException
import fujitsu.takehome.transferapp.exceptions.NotFoundException
import fujitsu.takehome.transferapp.repository.AccountsRepository
import fujitsu.takehome.transferapp.repository.TransactionRepository
import fujitsu.takehome.transferapp.utils.CurrencyUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountServiceImpl(
    private val accountsRepository: AccountsRepository,
    private val transferRepository: TransactionRepository
): AccountService {

    override fun getAccountDetails(id: Long): AccountDetails =
        accountsRepository.getAccountDetails(id)
        ?: throw NotFoundException(message = "account $id not found")

    override fun getAccountTransactionHistories(id: Long, lastIdSeen: Long?, limit: Int): TransactionHistories {
        val histories = transferRepository.getAccountTransactionHistories(
            accountId = id,
            lastIdSeen = lastIdSeen,
            limit = limit + 1
        )
        val hasMore = histories.size > limit

        return TransactionHistories(
            transactions = (if (hasMore) histories.dropLast(1) else histories).map {
                val amount: Long = if (it.fromAccountId == id) {
                    it.amount * -1
                } else {
                    it.amount
                }
                TransactionHistory(
                    id = it.id,
                    accountId = id,
                    amount = CurrencyUtil.formatAmount(amount, it.currency).toString(),
                    createdAt = it.createdAt,
                    currency = it.currency
                )
            },
            hasMore
        )
    }

    private fun TransferRequestDto.toAccountDetails(): Pair<AccountTransfer, AccountTransfer> {
        return Pair(
            AccountTransfer(fromAccount, amount * -1),
            AccountTransfer(toAccount, amount)
        )
    }

    override fun checkDuplicateTransactionRequest(request: TransferRequestDto): Boolean {
        val transaction = transferRepository.getTransactionByIdempotencyKey(request.uniqueId) ?: throw BadRequestException(message = "Invalid request was made. Please make sure the data was correct")
        if (
            request.fromAccount != transaction.fromAccountId ||
            request.toAccount != transaction.toAccountId ||
            request.amount != transaction.amount ||
            request.currency != transaction.currency
        )  {
            throw BadRequestException(message = "mismatched request. Please create a new one")
        }
        return true
    }

    // TODO: Mask sensitive info
    @Transactional
    override fun transferBetweenAccounts(request: TransferRequestDto) {
        transferRepository.createTransaction(request)
        val (fromAccount, toAccount) = request.toAccountDetails()
        val account1: AccountTransfer
        val account2: AccountTransfer
        // put the lower id first to attempt to avoid deadlocks.
        if (fromAccount.accountId < toAccount.accountId) {
            account1 = fromAccount
            account2 = toAccount
        } else {
            account1 = toAccount
            account2 = fromAccount
        }
        if (!accountsRepository.updateAccountBalance(account1)) {
            logger.error { "$account1 failed to update. Request: $request" }
            throw DatabaseErrorException(message = "Couldn't update the account balance")
        }
        if (!accountsRepository.updateAccountBalance(account2)) {
            logger.error { "$account2 failed to update. Request: $request" }
            throw DatabaseErrorException(message = "Couldn't update the account balance")
        }
    }
    companion object {
        private val logger = KotlinLogging.logger {  }
    }
}