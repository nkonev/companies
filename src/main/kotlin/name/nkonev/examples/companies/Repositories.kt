package name.nkonev.examples.companies

import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface CompanyRepository: PagingAndSortingRepository<Company, UUID>

interface LegalEntityRepository: PagingAndSortingRepository<LegalEntity, UUID>

interface MappingRepository: PagingAndSortingRepository<Mapping, UUID> {
    fun findByCompanyId(companyId: UUID): List<Mapping>

}