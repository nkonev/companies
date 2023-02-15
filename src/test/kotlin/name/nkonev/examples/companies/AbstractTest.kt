package name.nkonev.examples.companies

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

const val MYSQL_PORT = 3306

// Those tests work with database from testcontainers
@SpringBootTest(classes = [CompaniesApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
abstract class AbstractTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var storageService: StorageService

    companion object {
        val database = GenericContainer<Nothing>(DockerImageName.parse("dolthub/dolt-sql-server:0.53.0")).apply {
            withClasspathResourceMapping("/init.sql", "/docker-entrypoint-initdb.d/init.sql", BindMode.READ_ONLY)
            withExposedPorts(MYSQL_PORT)
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:mysql://${database.host}:${database.getMappedPort(MYSQL_PORT)}/companies?autoReconnect=true&useSSL=false" }
            registry.add("spring.datasource.username") { "root" }
        }

        val testUserId = UUID.randomUUID()
    }

}