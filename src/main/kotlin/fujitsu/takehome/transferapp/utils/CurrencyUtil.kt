package fujitsu.takehome.transferapp.utils

import fujitsu.takehome.transferapp.constants.USD
import java.math.BigDecimal

object CurrencyUtil {
    //    TODO: Don't hard code it.
    private val currencyToDecimal = mapOf(USD to 2)

    fun formatAmount(amount: Long, currency: String): BigDecimal {
        return BigDecimal.valueOf(amount, currencyToDecimal[currency.uppercase()] ?: throw IllegalStateException("Unknown currency $currency"))
    }
}