package fujitsu.takehome.transferapp

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.ZoneOffset
import java.util.*

@SpringBootApplication
class TransferappApplication {

    @PostConstruct
    fun setTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}

fun main(args: Array<String>) {
    runApplication<TransferappApplication>(*args)
}
