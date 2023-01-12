package name.nkonev.examples.companies

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@EnabledIfSystemProperty(named = "enableLoadTests", matches = "true")
@SpringBootTest(classes = [CompaniesApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class VolumeTest {

    @Autowired
    lateinit var companiesController: CompaniesController // it is real java service, no http interaction during call

    @Autowired
    lateinit var storageService: StorageService

    private val logger = LoggerFactory.getLogger(this::class.java)

    val testUserId = UUID.randomUUID()

    @Test
    fun thirty_five_thousands_branches() {
        val company = Company(UUID.randomUUID(), "Company one", new = true)
        val savedCompany = companiesController.createCompany(company, testUserId)
        logger.info("Created company with name ${savedCompany.name}, id ${savedCompany.identifier}")

        val max = 35000
        for (i in 1..max) {
            val branchName = UUID.randomUUID().toString()
            storageService.createAndCheckoutBranch(branchName)
            logger.info("$i of $max - Created branch with name ${branchName}")
        }
    }

}