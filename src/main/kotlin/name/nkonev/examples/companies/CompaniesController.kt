package name.nkonev.examples.companies

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.web.bind.annotation.*
import java.util.*

const val USER_ID_HEADER = "X-Userid"
const val DEFAULT_PAGE = 0
const val DEFAULT_SIZE = 20
const val DEFAULT_SORT_BY = "name"

@RestController
class CompaniesController(
    val companyRepository: CompanyRepository,
    val storageService: StorageService,
    val mappingRepository: MappingRepository,
    val legalEntityRepository: LegalEntityRepository
) {

    @GetMapping("/company")
    fun getCompanies(
        @RequestParam(name = "page", defaultValue = DEFAULT_PAGE.toString()) page: Int,
        @RequestParam(name = "size", defaultValue = DEFAULT_SIZE.toString()) size: Int,
        @RequestParam(name = "sortBy", defaultValue = DEFAULT_SORT_BY) sortBy: String
    ): List<Company> {
        return storageService.executeInBranch(MAIN_BRANCH) { companyRepository.findAll(PageRequest.of(page, size, Sort.by(sortBy))).toList() }
    }

    @PostMapping("/company")
    fun createCompany(@RequestBody body: Company, @RequestHeader(USER_ID_HEADER) userId: UUID): Company {
        return storageService.executeInBranch(MAIN_BRANCH) {
            val newBody = body.copy(new = true)
            val saved = companyRepository.save(newBody)
            storageService.addAndCommit(userId, "Created new company")
            return@executeInBranch saved
        }
    }

    @GetMapping("/company/{id}/drafts")
    fun getBranches(@PathVariable("id") companyId: UUID): List<UUID> {
        return storageService.executeInBranch(MAIN_BRANCH) {
            return@executeInBranch mappingRepository.findByCompanyId(companyId).map { mapping -> mapping.identifier }
        }
    }

    data class DraftResponse(
        val draftId: UUID,
        val company: Company
    )

    @PostMapping("/company/{id}/draft")
    fun createDraft(@PathVariable("id") companyId: UUID, @RequestHeader(USER_ID_HEADER) userId: UUID): DraftResponse {
        return storageService.executeInBranch(MAIN_BRANCH) {
            val draftId = UUID.randomUUID()
            val branchName = draftId.toString() // also transactionId

            val company = companyRepository.findById(companyId).orElseThrow()

            storageService.createAndCheckoutBranch(branchName)

            storageService.checkoutBranch(MAIN_BRANCH)
            mappingRepository.save(Mapping(draftId, userId, company.identifier, true))
            storageService.addAndCommit(userId, "Saved draft mapping draftId=$draftId")
            return@executeInBranch DraftResponse(draftId, company)
        }
    }

    data class EditDraft(val message: String, val company: Company)

    @PutMapping("/company/{id}/draft/{draftId}")
    fun editDraftedCompany(@PathVariable("id") companyId: UUID, @PathVariable("draftId") draftId: UUID, @RequestHeader(USER_ID_HEADER) userId: UUID, @RequestBody body: EditDraft) : Company {
        return storageService.executeInBranch(draftId.toString()) {
            val newBody = body.company.copy(identifier = companyId)
            val saved: Company = companyRepository.save(newBody)
            storageService.addAndCommit(userId, body.message)
            return@executeInBranch saved
        }
    }


    data class AddLegalEntity(val message: String, val legalEntity: LegalEntity)

    @PutMapping("/company/{id}/draft/{draftId}/legal-entity")
    fun editDraftedCompanyAddLegalEntity(@PathVariable("id") companyId: UUID, @PathVariable("draftId") draftId: UUID, @RequestHeader(USER_ID_HEADER) userId: UUID, @RequestBody body: AddLegalEntity) : Company {
        return storageService.executeInBranch(draftId.toString()) {
            val company = companyRepository.findById(companyId).orElseThrow()
            val newBody = body.legalEntity.copy(new = true, companyId = companyId)
            company.legalEntities = company.legalEntities.plus(newBody)
            val saved: Company = companyRepository.save(company)
            storageService.addAndCommit(userId, body.message)
            return@executeInBranch saved
        }
    }

    data class HistoryResponse(val id: String, val userId: UUID, val message: String)

    @GetMapping("/company/{id}/draft/{draftId}/history")
    fun getDraftHistory(@PathVariable("id") companyId: UUID, @PathVariable("draftId") draftId: UUID) : List<HistoryResponse> {
        return storageService.executeInBranch(draftId.toString()) {
            val histories: List<HistoryResponse> = storageService.getHistoryFrom(MAIN_BRANCH, draftId.toString())
            return@executeInBranch histories
        }
    }

    @GetMapping("/company/{id}/draft/{draftId}")
    fun getCompanyInBranch(@PathVariable("id") companyId: UUID, @PathVariable("draftId") draftId: UUID): Company {
        return storageService.executeInBranch(draftId.toString()) {
            return@executeInBranch companyRepository.findById(companyId).orElseThrow()
        }
    }

    @GetMapping("/company/{id}")
    fun getCompanyInMainBranch(@PathVariable("id") companyId: UUID): Company {
        return storageService.executeInBranch(MAIN_BRANCH) {
            return@executeInBranch companyRepository.findById(companyId).orElseThrow()
        }
    }

    data class ApproveDraft(val message: String)

    @PutMapping("/company/{id}/draft/{draftId}/approve")
    fun approveDraft(@PathVariable("id") companyId: UUID, @PathVariable("draftId") draftId: UUID, @RequestHeader(USER_ID_HEADER) userId: UUID, @RequestBody body: ApproveDraft) : ResponseEntity<Company> {
        try {
            return storageService.executeInBranch(MAIN_BRANCH) {
                storageService.mergeBranch(userId, draftId.toString(), body.message)

                mappingRepository.deleteById(draftId)
                storageService.addAndCommit(userId, "Remove draft mapping for merged draftId=$draftId")

                storageService.deleteBranch(draftId.toString())

                val saved: Company = companyRepository.findById(companyId).orElseThrow()
                return@executeInBranch ResponseEntity.ok(saved)
            }
        } catch (ex: UncategorizedSQLException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

}
