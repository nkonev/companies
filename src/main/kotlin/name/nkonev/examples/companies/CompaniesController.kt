package name.nkonev.examples.companies

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.util.*

const val USER_ID_HEADER = "X-Userid"
const val DEFAULT_PAGE = 0
const val DEFAULT_SIZE = 20
const val DEFAULT_SORT_BY = "name"

@RestController
class CompaniesController(
    val companyRepository: CompanyRepository,
) {

    @GetMapping("/company")
    fun getCompanies(
        @RequestParam(name = "page", defaultValue = DEFAULT_PAGE.toString()) page: Int,
        @RequestParam(name = "size", defaultValue = DEFAULT_SIZE.toString()) size: Int,
        @RequestParam(name = "sortBy", defaultValue = DEFAULT_SORT_BY) sortBy: String
    ): List<Company> {
        return companyRepository.findAll(PageRequest.of(page, size, Sort.by(sortBy))).toList()
    }

    @PostMapping("/company")
    fun createCompany(@RequestBody body: Company, @RequestHeader(USER_ID_HEADER) userId: UUID): Company {
        val newBody = body.copy(new = true)
        val saved = companyRepository.save(newBody)
        return saved
    }

}
