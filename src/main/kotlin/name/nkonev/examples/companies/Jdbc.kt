package name.nkonev.examples.companies

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.LocalDateTime
import java.util.*

@Table(name = "company")
data class Company(
    @Id @Column("id") @JsonProperty("id") val identifier: UUID = UUID.randomUUID(),
    val name: String,
    var bankAccount: String? = null,
    var estimatedSize: Int? = null,
    @LastModifiedDate var modifiedAt: LocalDateTime? = null,
    var metadata: JsonNode = NullNode.instance,
    @MappedCollection(keyColumn = "company_id", idColumn = "company_id")
    var legalEntities: Set<LegalEntity> = mutableSetOf(),
    @Transient @JsonIgnore val new: Boolean = false
) : Persistable<UUID> {

    @JsonIgnore
    override fun getId(): UUID {
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
    ) : this(identifier, name, bankAccount, estimatedSize, modifiedAt, NullNode.instance, mutableSetOf(), false)
}

interface CompanyRepository: CrudRepository<Company, UUID> {
    fun findAll(pageable: Pageable): List<Company>
}

data class LegalEntity(
    @Id @Column("id") @JsonProperty("id") val identifier: UUID = UUID.randomUUID(),
    var name: String,
    var country: String,
    @Transient @JsonIgnore val new: Boolean = false
): Persistable<UUID> {
    @JsonIgnore
    override fun getId(): UUID {
        return identifier
    }

    @JsonIgnore
    override fun isNew(): Boolean {
        return new
    }

    @PersistenceCreator
    constructor(identifier: UUID, name: String, country: String): this(identifier, name, country, false)
}

interface LegalEntityRepository: CrudRepository<LegalEntity, UUID>

@Table(name = "mapping")
data class Mapping(
    @Id @Column("branch_id") @JsonProperty("branchId") val identifier: UUID = UUID.randomUUID(),
    val userId: UUID,
    val companyId: UUID,
    @Transient @JsonIgnore val new: Boolean = false
) : Persistable<UUID> {
    @JsonIgnore
    override fun getId(): UUID {
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
        userId: UUID,
        companyId: UUID
    ) : this(identifier, userId, companyId, false)
}

interface MappingRepository: CrudRepository<Mapping, UUID> {
    fun findByCompanyId(companyId: UUID): List<Mapping>

}