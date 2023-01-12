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

@EnabledIfSystemProperty(named = "enableLoadTests", matches = "true")
// You need run this test against the started application
@SpringBootTest(classes = [CompaniesApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class VolumeTest {

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val logger = LoggerFactory.getLogger(this::class.java)

    val restTemplate = TestRestTemplate()

    @Value("\${server.port:8080}")
    lateinit var port: Integer

    val testUserId = UUID.randomUUID()

    @Test
    fun `seven thousands suppliers`() {

        val timeBefore = measureTime {
            val request = RequestEntity.get(URI.create("http://localhost:${port}/company"))
                .build()
            restTemplate.exchange(request, String::class.java)
        }
        logger.info("Request time before ${timeBefore}")

        val insertionTime = measureTime {
            storageService.checkoutBranch(MAIN_BRANCH)
            for (i in 1..7000) {
                val company = Company(UUID.randomUUID(), "Company number $i", new = true)
                val savedCompany = companyRepository.save(company)
            }
            storageService.addAndCommit(testUserId, "Save all the companies")
        }
        logger.info("Insertion time ${insertionTime}")

        val timeAfter = measureTime {
            val get = RequestEntity.get(URI.create("http://localhost:${port}/company"))
                .build()
            restTemplate.exchange(get, String::class.java)
        }
        logger.info("Request time after ${timeAfter}")
    }

    @Test
    fun `one hundred users`() {
        storageService.checkoutBranch(MAIN_BRANCH)

        val companies = companyRepository.findAll()

        val measuredTime = measureTime {

            var i = 0
            val iter = companies.iterator()
            // create the draft(branch) per every user
            while(iter.hasNext()) {
                i++
                val company = iter.next()
                logger.info("Writing ${i} company id=${company.identifier}")
                val request1 = RequestEntity.post(URI.create("http://localhost:${port}/company/${company.identifier}/draft"))
                    .header(USER_ID_HEADER, testUserId.toString())
                    .build()
                val response1 = restTemplate.exchange(request1, String::class.java)
                logger.info("Create draft response: ${response1.statusCode}")

                val responseObj1: CompaniesController.DraftResponse = objectMapper.readValue(response1.body!!)

                val request2 = RequestEntity.put(URI.create("http://localhost:${port}/company/${company.identifier}/draft/${responseObj1.draftId}"))
                    .header(USER_ID_HEADER, testUserId.toString())
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(objectMapper.writeValueAsString(CompaniesController.EditDraft("A change $i", company.copy(name = "Changed company $i"))))
                val response2 = restTemplate.exchange(request2, String::class.java)
                logger.info("Edit draft response: ${response2.statusCode}")
            }
        }
        logger.info("Measured time ${measuredTime}")
    }

    private fun measureTime(runnable: Runnable): Duration {
        val first = Instant.now()
        runnable.run()
        val second = Instant.now()
        return Duration.between(first, second)
    }
}