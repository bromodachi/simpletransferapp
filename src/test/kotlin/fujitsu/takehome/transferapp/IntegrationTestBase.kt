package fujitsu.takehome.transferapp

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.util.CollectionUtils
import org.springframework.util.FileCopyUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.LinkedHashMap

@ActiveProfiles("test")
@TestExecutionListeners(DependencyInjectionTestExecutionListener::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTestBase {
    protected val LEDGER_ENDPOINT = "/ledger"
    protected val LEDGER_DETAILS_ENDPOINT = "/ledger/{id}/details"

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper


    @PostConstruct
    private fun initSetup() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
    }

    protected fun resourceToString(resource: Resource): String? {
        try {
            InputStreamReader(resource.inputStream, StandardCharsets.UTF_8).use { reader ->
                return FileCopyUtils.copyToString(
                    reader
                )
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun toMultiMap(queryParams: Map<String, Any>): MultiValueMap<String, String>? {
        val newMap: MutableMap<String, List<String>> = LinkedHashMap()
        for ((key, value) in queryParams.entries) {
            newMap[key] = listOf(value.toString())
        }
        return CollectionUtils.toMultiValueMap(newMap)
    }

    protected fun getRequest(
        path: String?,
        queryParams: Map<String, Any>
    ): ResponseEntity<String?>? {
        val components = UriComponentsBuilder.fromPath(path!!)
            .queryParams(toMultiMap(queryParams)).build()
        return testRestTemplate!!.exchange(
            components.toString(),
            HttpMethod.GET,
            null,
            String::class.java
        )
    }

    protected fun getRequest(
        path: String?,
        queryParams: Map<String, Any>,
        headers: HttpHeaders
    ): ResponseEntity<String?>? {
        val components = UriComponentsBuilder.fromPath(path!!)
            .queryParams(toMultiMap(queryParams)).build()
        return testRestTemplate!!.exchange(
            components.toString(),
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            String::class.java
        )
    }

    @Throws(JsonProcessingException::class)
    protected fun postRequest(
        path: String?,
        body: String?
    ): ResponseEntity<String?>? {
        val components = UriComponentsBuilder.fromPath(path!!).build()
        return testRestTemplate!!.exchange(
            components.toString(),
            HttpMethod.POST,
            HttpEntity(objectMapper!!.readTree(body)),
            String::class.java
        )
    }

    protected fun postRequest(
        path: String?,
        body: Any
    ): ResponseEntity<String?>? {
        val components = UriComponentsBuilder.fromPath(path!!).build()
        return testRestTemplate!!.exchange(
            components.toString(),
            HttpMethod.POST,
            HttpEntity(body),
            String::class.java
        )
    }

    protected fun postRequest(
        path: String?,
        body: Any?,
        headers: HttpHeaders?
    ): ResponseEntity<String?>? {
        val components = UriComponentsBuilder.fromPath(path!!).build()
        return testRestTemplate!!.exchange(
            components.toString(),
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
    }
}