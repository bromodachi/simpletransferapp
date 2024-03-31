package fujitsu.takehome.transferapp.controller

import fujitsu.takehome.transferapp.constants.USD
import fujitsu.takehome.transferapp.dto.request.TransferRequestDto
import fujitsu.takehome.transferapp.dto.response.AccountDetails
import fujitsu.takehome.transferapp.dto.response.TransactionHistories
import fujitsu.takehome.transferapp.dto.response.TransactionHistory
import fujitsu.takehome.transferapp.exceptions.BadRequestException
import fujitsu.takehome.transferapp.service.AccountService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val accountService: AccountService
) {

    @GetMapping("/{id}")
    fun getAccount(
        @PathVariable id: Long,
    ): AccountDetails {
        val result = accountService.getAccountDetails(id)
        return result
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    fun transfer(@RequestBody @Validated request: TransferRequestDto) {
        if (request.toAccount == request.fromAccount) {
            throw BadRequestException(message = "You can't transfer money to the same account!")
        }
        if (request.currency != USD) {
            throw BadRequestException(message = "This is a not full-fledged app! We only accept USD atm")
        }
        try {
            accountService.transferBetweenAccounts(request)
        } catch (e: DuplicateKeyException) {
            accountService.checkDuplicateTransactionRequest(request)
        } catch (e: DataIntegrityViolationException) {
            // TODO: Give a better error message
            logger.warn(e) { "DataIntegrityViolationException for request $request" }
            throw BadRequestException(message = "Invalid $request. Please adjust it.")
        }catch (e: Exception) {
            logger.error(e) {  }
            throw e
        }
    }


    @GetMapping("{id}/transfers")
    fun getTransferHistories(
        @PathVariable id: Long,
        @RequestParam(value = "lastId", required = false) lastIdSeen: Long? = null,
        @RequestParam(value = "size", required = false, defaultValue = "25") limit: Int
    ): TransactionHistories {
        if (limit < 0 || limit > 1000) {
            throw BadRequestException(message = "Please pass in a valid limit that's greater than 0 but less than or equal to 1000.")
        }
        return accountService.getAccountTransactionHistories(id, lastIdSeen, limit)
    }

    companion object {
        private val logger = KotlinLogging.logger {  }
    }
}