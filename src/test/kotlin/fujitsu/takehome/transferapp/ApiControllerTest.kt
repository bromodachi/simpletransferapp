package fujitsu.takehome.transferapp

import fujitsu.takehome.transferapp.constants.USD
import fujitsu.takehome.transferapp.dto.request.TransferRequestDto
import fujitsu.takehome.transferapp.dto.response.AccountDetails
import fujitsu.takehome.transferapp.dto.response.TransactionHistories
import fujitsu.takehome.transferapp.entity.Account
import fujitsu.takehome.transferapp.entity.Transaction
import fujitsu.takehome.transferapp.repository.mapper.AccountsMapper
import fujitsu.takehome.transferapp.utils.CurrencyUtil
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.jdbc.Sql
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

@TestExecutionListeners
@Sql(scripts = ["/resources/db/migration/V1__CREATE_TABLES.sql"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiControllerTest: IntegrationTestBase() {

    // TODO: Ideally, you clear after each test. Taking shortcuts right now
    @Autowired
    lateinit var accountsMapper: AccountsMapper

    private fun Account.toAccountDetails(newBalance: Long? = null) = AccountDetails(
        id,
        userId,
        CurrencyUtil.formatAmount(newBalance ?: balance, currency).toString(),
        currency
    )

    private fun createAccount(userId: Long, amount: Long = TEN_THOUSAND) =
        Account(userId = userId, balance = amount, currency = USD)
            .apply(accountsMapper::insertAccount)

    private fun createBobAccount(amount: Long = TEN_THOUSAND): Account =
        createAccount(userId = 3, amount = amount)

    private fun createAliceAccount(amount: Long = TEN_THOUSAND): Account =
        createAccount(userId = 4, amount = amount)

    @Test
    fun getAccountForExistingAccount() {
        // Arrange
        val bobAccount = createBobAccount()
        // Act
        val (response, details) = getAccountDetails(bobAccount.id)

        // Assert
        Assertions.assertEquals(200, response?.statusCode?.value())

        Assertions.assertEquals(
            bobAccount.toAccountDetails(),
            details,
        )
    }

    @Test
    fun `get account for no existing - return 400`() {
        // Act
        val (response, _) = getAccountDetails(-1)

        // Assert
        Assertions.assertEquals(400, response?.statusCode?.value())
    }

    @Test
    fun `get transfer history for non-existing user - should be empty`() {
        // Act
        val (response, histories) = getTransferHistories(400)
        // Assert
        Assertions.assertEquals(200, response?.statusCode?.value())
        Assertions.assertNotNull(histories)
        Assertions.assertEquals(false, histories?.hasMore)
        Assertions.assertTrue(histories?.transactions?.isEmpty() ?: false)
    }

    @Test
    fun `bob creates transaction - bob inputs the wrong account`() {
        // Arrange & Act
        val response = createTransaction(
            createBobAccount().let {
                TransferRequestDto(fromAccount = it.id, toAccount = Int.MAX_VALUE.toLong(), 500L, UUID.randomUUID().toString(), USD)
            }
        )
        // Assert
        Assertions.assertEquals(400, response?.statusCode?.value())
    }

    @Test
    fun `bob creates transaction - sends five dollars to alice`() {
        // Each have one hundred dollars
        // Arrange - Part 1
        val bobAccount = createBobAccount()
        val aliceAccount = createAliceAccount()
        // Act - Part 1
        val response = createTransaction(
            TransferRequestDto(fromAccount = bobAccount.id, toAccount = aliceAccount.id, 500L, UUID.randomUUID().toString(), USD)
        )
        // Assert - Part 1
        Assertions.assertEquals(201, response?.statusCode?.value())

        // Act - Part 2
        val (_, histories) = getTransferHistories(id = bobAccount.id)

        // Assert- Part 2
        Assertions.assertNotNull(histories)
        Assertions.assertFalse(histories!!.transactions.isEmpty())
        val transaction = histories.transactions.first()
        Assertions.assertEquals(
            bobAccount.id,
            transaction.accountId
        )
        Assertions.assertEquals(
            "-5.00",
            transaction.amount
        )
        Assertions.assertEquals(
            USD,
            transaction.currency
        )

        val (responseAccountDetails, details) = getAccountDetails(bobAccount.id)

        Assertions.assertEquals(200, responseAccountDetails?.statusCode?.value())

        Assertions.assertEquals(
            bobAccount.toAccountDetails(TEN_THOUSAND - 500),
            details,
        )
    }

    @RepeatedTest(3)
    fun `bob and alice have a joint account - sending multiple requests to charlie`() {
        // joint account only has 5 dollars
        val bobAndAliceJointAccount = createAccount(4, 500)
        // charlie has no money
        val charlie = createAccount(5, 0)
        val executorService = Executors.newFixedThreadPool(4)
        val completionService = ExecutorCompletionService<ResponseEntity<String?>?>(executorService)

        var count = 3
        for (i in 1..count) {
            completionService.submit {
                createTransaction(
                    TransferRequestDto(
                        fromAccount = bobAndAliceJointAccount.id,
                        toAccount = charlie.id,
                        300L,
                        UUID.randomUUID().toString(), USD
                    )
                )
            }
        }

        var successCount = 0
        var failedCount = 0
        while (count > 0) {
            val someFuture = completionService.take()
            val result = someFuture.get()
            if (result?.statusCode?.value() == 201) {
                successCount += 1
            }
            else {
                failedCount += 1
            }
            count -=1
        }
        Assertions.assertEquals(1, successCount)
        Assertions.assertEquals(2, failedCount)
        val (_, histories) = getTransferHistories(id = bobAndAliceJointAccount.id)
        Assertions.assertNotNull(histories)
        Assertions.assertFalse(histories!!.transactions.isEmpty())
        val transaction = histories.transactions.first()
        Assertions.assertEquals(
            bobAndAliceJointAccount.id,
            transaction.accountId
        )
        Assertions.assertEquals(
            "-3.00",
            transaction.amount
        )
    }

    @RepeatedTest(3)
    fun `try to dead lock - bob send money, alice send money`() {
        val bobAccount = createBobAccount(amount = 500)
        val aliceAccount = createAliceAccount(amount = 500)

        val executorService = Executors.newFixedThreadPool(4)
        val completionService = ExecutorCompletionService<ResponseEntity<String?>?>(executorService)

        // Act - Send the transaction request
        var count = 2
        for (i in 1..count) {
            completionService.submit {
                if (i.mod(2) == 0) {
                    createTransaction(
                        TransferRequestDto(
                            fromAccount = bobAccount.id,
                            toAccount = aliceAccount.id,
                            300L,
                            UUID.randomUUID().toString(), USD
                        )
                    )
                }
                else {
                    createTransaction(
                        TransferRequestDto(
                            fromAccount = aliceAccount.id,
                            toAccount = bobAccount.id,
                            300L,
                            UUID.randomUUID().toString(), USD
                        )
                    )
                }
            }
        }

        var successCount = 0
        var failedCount = 0
        while (count > 0) {
            val someFuture = completionService.take()
            val result = someFuture.get()
            if (result?.statusCode?.value() == 201) {
                successCount += 1
            }
            else {
                failedCount += 1
            }
            count -=1
        }
        // Assert
        Assertions.assertEquals(2, successCount)
        Assertions.assertEquals(0, failedCount)
        val (_, histories) = getTransferHistories(id = bobAccount.id)
        Assertions.assertNotNull(histories)
        Assertions.assertFalse(histories!!.transactions.isEmpty())
        val receivedMoneyTransaction = histories.transactions.firstOrNull { it.amount == "3.00" }
        val sendMoneyTransaction = histories.transactions.firstOrNull { it.amount == "-3.00" }
        Assertions.assertNotNull(receivedMoneyTransaction)
        Assertions.assertNotNull(sendMoneyTransaction)
        Assertions.assertEquals(
            bobAccount.id,
            // should always be bob
            receivedMoneyTransaction!!.accountId
        )
        Assertions.assertEquals(
            bobAccount.id,
            // should always be bob
            sendMoneyTransaction!!.accountId
        )
        Assertions.assertEquals(
            "3.00",
            receivedMoneyTransaction.amount
        )
        Assertions.assertEquals(
            "-3.00",
            sendMoneyTransaction.amount
        )
        val (responseAccountDetails, details) = getAccountDetails(bobAccount.id)

        Assertions.assertEquals(200, responseAccountDetails?.statusCode?.value())

        Assertions.assertEquals(
            // will still be $5
            bobAccount.toAccountDetails(500),
            details,
        )
    }

    @Test
    fun `bob makes multiple transaction - pagination test`() {
        // Arrange
        val bobAccount = createBobAccount(amount = 500)
        val aliceAccount = createAliceAccount(amount = 500)

        val executorService = Executors.newFixedThreadPool(4)
        val completionService = ExecutorCompletionService<ResponseEntity<String?>?>(executorService)

        // Act - Send the transaction request
        var count = 3
        for (i in 1..count) {
            completionService.submit {
                createTransaction(
                    TransferRequestDto(
                        fromAccount = bobAccount.id,
                        toAccount = aliceAccount.id,
                        // Just send one dollar at a time
                        100L,
                        UUID.randomUUID().toString(),
                        USD
                    )
                )
            }
        }
        var successCount = 0
        var failedCount = 0
        while (count > 0) {
            val someFuture = completionService.take()
            val result = someFuture.get()
            if (result?.statusCode?.value() == 201) {
                successCount += 1
            }
            else {
                failedCount += 1
            }
            count -=1
        }

        // Assert
        Assertions.assertEquals(3, successCount)
        Assertions.assertEquals(0, failedCount)
        val (_, bobHistories) = getTransferHistories(id = bobAccount.id)
        val (_, aliceHistories) = getTransferHistories(id = aliceAccount.id)

        // all negative $1
        val bobAllNegative = bobHistories?.transactions?.all { it.amount == "-1.00" } ?: false
        Assertions.assertTrue(bobAllNegative)

        // all positive $1
        val aliceAllPositive = aliceHistories?.transactions?.all { it.amount == "1.00" } ?: false
        Assertions.assertTrue(aliceAllPositive)


        // test pagination real quick
        val (_, bobHistoriesPag) = getTransferHistories(id = bobAccount.id, limit = 2)
        Assertions.assertEquals(2, bobHistoriesPag?.transactions?.size)
        Assertions.assertEquals(true, bobHistoriesPag?.hasMore)

        val (_, bobHistoriesPag2) = getTransferHistories(id = bobAccount.id, limit = 2, lastIdSeen = bobHistoriesPag?.transactions?.last()?.id)
        Assertions.assertEquals(1, bobHistoriesPag2?.transactions?.size)
        Assertions.assertEquals(false, bobHistoriesPag2?.hasMore)
    }

    private fun getAccountDetails(
        id: Long,
        params: Map<String, String> = mapOf()
    ): Pair<ResponseEntity<String?>? , AccountDetails?> {
        val responseEntity = getRequest("$ACCOUNTS_BASE/$id", params)
        return Pair(
            responseEntity,
            responseEntity?.takeIf { it.statusCode.value() == 200 }?.let { response ->
                response.body?.let {
                    objectMapper.readValue(it, AccountDetails::class.java)
                }
        })
    }

    private fun createTransaction(
        request: TransferRequestDto
    ): ResponseEntity<String?>? {
        return postRequest("$ACCOUNTS_BASE/transfer", request)
    }

    private fun getTransferHistories(
        id: Long,
        lastIdSeen: Long? = null,
        limit: Int? = null
    ): Pair<ResponseEntity<String?>? , TransactionHistories?> {
        val params = buildMap<String, String> {
            lastIdSeen?.let { put("lastId", "$it") }
            limit?.let { put("size", "$it") }
        }
        val responseEntity = getRequest("$ACCOUNTS_BASE/$id/transfers", params)
        return Pair(
            responseEntity,
            responseEntity?.takeIf { it.statusCode.value() == 200 }?.let { response ->
                response.body?.let {
                    objectMapper.readValue(it, TransactionHistories::class.java)
                }
            })
    }

    companion object {

        private const val ACCOUNTS_BASE = "/accounts"

        private const val TEN_THOUSAND: Long = 10_000 * 100


        private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:14.11"))

        @BeforeAll
        @JvmStatic
        fun setup() {
            postgres.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            postgres.stop()
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerDBContainer(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}