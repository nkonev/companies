package name.nkonev.examples.companies

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.LocalDateTime
import java.util.*

@Table(name = "company")
data class Company(
    @Id @Column("id") @JsonProperty("id") val identifier: UUID? = null,
    val name: String,
    var bankAccount: String? = null,
    var estimatedSize: Int? = null,
    @LastModifiedDate var modifiedAt: LocalDateTime? = null,
    @Transient @JsonIgnore val new: Boolean = false
) : Persistable<UUID> {

    @JsonIgnore
    override fun getId(): UUID? {
        return identifier
    }

    @JsonIgnore
    override fun isNew(): Boolean {
        return new
    }

    // Fixes Required property new not found for class
    @PersistenceCreator
    constructor(
        identifier: UUID,
        name: String,
        bankAccount: String?,
        estimatedSize: Int?,
        modifiedAt: LocalDateTime?
    ) : this(identifier, name, bankAccount, estimatedSize, modifiedAt, false)
}

interface CompanyRepository: CrudRepository<Company, UUID> {
    fun findAll(pageable: Pageable): List<Company>
}
