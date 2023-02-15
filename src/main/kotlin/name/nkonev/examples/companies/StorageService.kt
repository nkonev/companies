package name.nkonev.examples.companies

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

const val MAIN_BRANCH = "main"

@Service
class StorageService(val jdbcTemplate: JdbcTemplate, val transactionTemplate: TransactionTemplate) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun <T> executeInBranchNullable(branchName: String, action: TransactionCallback<T>): T? {
        return transactionTemplate.execute {
            checkoutBranch(branchName)
            action.doInTransaction(it)
        }
    }

    fun <T> executeInBranch(branchName: String, action: TransactionCallback<T>): T {
        return executeInBranchNullable(branchName, action)!!
    }

    fun checkoutBranch(name: String) {
        jdbcTemplate.update("CALL DOLT_CHECKOUT (?)", name)
    }

    fun getAllBranches(): List<String> {
        return jdbcTemplate.queryForList("select name from dolt_branches", String::class.java)
    }

    fun deleteBranch(name: String) {
        jdbcTemplate.update("CALL DOLT_BRANCH ('-d', ?)", name)
    }

    fun forceDeleteBranch(name: String) {
        logger.info("Forcibly deleting branch {}", name)
        jdbcTemplate.update("CALL DOLT_BRANCH ('-d', ?, '--force')", name)
    }

    fun createAndCheckoutBranch(name: String) {
        jdbcTemplate.update("CALL DOLT_CHECKOUT ('-b', ?)", name)
    }

    private fun createUsername(userId: UUID): String {
        return "$userId <$userId@example.com>"
    }

    fun addAndCommit(userId: UUID, message: String) {
        val username = createUsername(userId)
        jdbcTemplate.update("CALL DOLT_ADD('.')")
        jdbcTemplate.update("CALL DOLT_COMMIT ('-m', ?, '--author', ?)", message, username)
    }

    /**
     * Merges provided branch into current
     */
    fun mergeBranch(userId: UUID, name: String, message: String) {
        val username = createUsername(userId)
        jdbcTemplate.update("CALL DOLT_MERGE (?, '-m', ?, '--author', ?)", name, message, username)
    }

    private data class InternalHistoryResponse(val id: String, val userId: String, val message: String)

    fun getHistoryFrom(mainBranch: String, draftBranch: String): List<CompaniesController.HistoryResponse> {
        val history = jdbcTemplate.query("SELECT * FROM DOLT_LOG('$mainBranch..$draftBranch') ORDER BY date ASC") { rs, _ ->
            val committer = rs.getString("committer")
            val message = rs.getString("message")
            val hash = rs.getString("commit_hash")
            InternalHistoryResponse(id = hash, userId = committer, message = message)
        }

        return history
            .mapNotNull {
                try {
                    CompaniesController.HistoryResponse(id = it.id, userId = UUID.fromString(it.userId), message = it.message)
                } catch (ex: IllegalArgumentException) {
                    logger.warn("Unable to parse userId for {}, skipping", it)
                    null
                }
            }
    }
}