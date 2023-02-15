package name.nkonev.examples.companies

import liquibase.integration.spring.SpringLiquibase
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@ConditionalOnProperty("spring.liquibase.enabled", matchIfMissing = true)
@Service
class MigrationService(
    private val liquibase: SpringLiquibase,
    private val storageService: StorageService
): CommandLineRunner, HealthIndicator {

    @Volatile
    private var allBranchesProcessed: Boolean = false

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun migrate() {
        liquibase.afterPropertiesSet()
    }

    override fun run(vararg args: String?) {
        val branches = storageService.getAllBranches()
        for (branchWithIndex in branches.withIndex()) {
            val branch = branchWithIndex.value
            val number = branchWithIndex.index + 1
            if (branch != MAIN_BRANCH) { // main is already migrated by Spring's LiquibaseAutoConfiguration
                logger.info("Migrating database for branch {}, {} / {}", branch, number, branches.size)
                storageService.checkoutBranch(branch)
                migrate()
            }
        }
        logger.info("Migrating database for branches is completed")
        allBranchesProcessed = true
    }

    override fun health(): Health {
        if (allBranchesProcessed) {
            return Health
                .up()
                .build()
        } else {
            return Health
                .down()
                .withDetail("message", "Migration of all database branches is not still completed")
                .build()
        }
    }
}