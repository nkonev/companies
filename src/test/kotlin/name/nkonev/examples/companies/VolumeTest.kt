package name.nkonev.examples.companies

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*

// This is separated test which works with database declared in application.yaml
@EnabledIfSystemProperty(named = "enableLoadTests", matches = "true")
@SpringBootTest(classes = [CompaniesApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class VolumeTest {

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var companiesController: CompaniesController // it is real java service, no http interaction during call

    private val logger = LoggerFactory.getLogger(this::class.java)

    val restTemplate = TestRestTemplate()

    @Value("\${server.port:8080}")
    lateinit var port: Integer

    val testUserId = UUID.randomUUID()

    @Test
    fun seven_thousand_companies() {

        val timeBefore = measureTime {
            val request = RequestEntity.get(URI.create("http://localhost:${port}/company"))
                .build()
            restTemplate.exchange(request, String::class.java)
        }
        logger.info("Request time before ${timeBefore}")

        val insertionTime = measureTime {
            for (i in 1..7000) {
                val company = Company(UUID.randomUUID(), "Company number $i", new = true)
                val savedCompany = companyRepository.save(company)
                logger.info("Creating company $i")
            }
        }
        logger.info("Insertion time ${insertionTime}")

        val timeAfter = measureTime {
            val get = RequestEntity.get(URI.create("http://localhost:${port}/company"))
                .build()
            restTemplate.exchange(get, String::class.java)
        }
        logger.info("Request time after ${timeAfter}")
    }

    private fun measureTime(runnable: Runnable): Duration {
        val first = Instant.now()
        runnable.run()
        val second = Instant.now()
        return Duration.between(first, second)
    }


}