package name.nkonev.examples.companies

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.RequestEntity
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*

// This is separated test which works with database declared in application.yaml
@EnabledIfSystemProperty(named = "enableLoadTests", matches = "true")
@SpringBootTest(classes = [CompaniesApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class VolumeTest {

    @Autowired
    lateinit var companiesController: CompaniesController // it is real java service, no http interaction during call

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    private val logger = LoggerFactory.getLogger(this::class.java)

    val testUserId = UUID.randomUUID()

    val restTemplate = TestRestTemplate()

    @Value("\${server.port:8080}")
    lateinit var port: Integer

    @Test
    fun fifty_thousands_branches() {
        val company = Company(UUID.randomUUID(), "Company one", new = true)
        val savedCompany = companiesController.createCompany(company, testUserId)
        logger.info("Created company with name ${savedCompany.name}, id ${savedCompany.identifier}")

        val max = 50_000
        for (i in 1..max) {
            val branchName = UUID.randomUUID().toString()
            storageService.createAndCheckoutBranch(branchName)
            logger.info("$i of $max - Created branch with name ${branchName}")
        }
    }

    @Test
    fun volume_test_suppliers() {

//        val timeBefore = measureTime {
//            val request = RequestEntity.get(URI.create("http://localhost:${port}/company"))
//                .build()
//            restTemplate.exchange(request, String::class.java)
//        }
//        logger.info("Request time before ${timeBefore}")

        storageService.checkoutBranch(MAIN_BRANCH)

        val insertionTime = measureTime {
            for (i in 1..7000) {
                transactionTemplate.executeWithoutResult {

                    val branch = "branch_${i}_${UUID.randomUUID()}"
                    logger.info("Creating ${i}, branch $branch")
                    storageService.createAndCheckoutBranch(branch)
                    val company = Company(UUID.randomUUID(), "Company number $i", new = true)
                    var savedCompany = companyRepository.save(company)
                    storageService.addAndCommit(testUserId, "Save a company $i initial")
                    savedCompany.new = false
                    for (j in 1..10) {
                        savedCompany.name = "savedCompany.name" + UUID.randomUUID()
                        savedCompany = companyRepository.save(savedCompany)
                        storageService.addAndCommit(testUserId, "Save a company $i, iteration ${j}")
                    }
                }
            }
        }
        logger.info("Insertion time ${insertionTime}")

//        val timeAfter = measureTime {
//            val get = RequestEntity.get(URI.create("http://localhost:${port}/company"))
//                .build()
//            restTemplate.exchange(get, String::class.java)
//        }
//        logger.info("Request time after ${timeAfter}")
    }

    private fun measureTime(runnable: Runnable): Duration {
        val first = Instant.now()
        runnable.run()
        val second = Instant.now()
        return Duration.between(first, second)
    }

}
