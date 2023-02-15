package name.nkonev.examples.companies

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.*

class CompaniesTests: AbstractTest() {

	// in future move to AbstractFunctionalTest
	@BeforeEach
	fun beforeEach() {
		storageService.executeInBranch(MAIN_BRANCH) {
			jdbcTemplate.update("DELETE FROM mapping")
			jdbcTemplate.update("DELETE FROM company")
			storageService.addAndCommit(testUserId, "Database is wiped before test")
		}
	}

	@Test
	fun `approving a company works`() {
		val userId = UUID.randomUUID()
		
		// add a company
		val companyResult = mockMvc.post("/company"){
			this.content = objectMapper.writeValueAsString(Company(name = "Third company"))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company") }
		}.andReturn()
		val company: Company = objectMapper.readValue(companyResult.response.contentAsString)
		val companyId = company.identifier

		// create a draft
		mockMvc.post("/company/$companyId/draft"){
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
		}

		val draftsResult = mockMvc.get("/company/$companyId/drafts").andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val draftIds : List<UUID> = objectMapper.readValue(draftsResult.response.contentAsString)
		Assertions.assertEquals(1, draftIds.size)

		// patch a draft
		mockMvc.put("/company/$companyId/draft/${draftIds[0]}"){
			this.content = objectMapper.writeValueAsString(CompaniesController.EditDraft("Patching 100505", Company(name = "Third company patched 100505")))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched 100505") }
		}

		// patch a draft - add a legal entity
		mockMvc.put("/company/$companyId/draft/${draftIds[0]}/legal-entity"){
			this.content = objectMapper.writeValueAsString(CompaniesController.AddLegalEntity("Patching 100505 legal entity", LegalEntity(name = "Third company patched 100505", country = "Land of Freedom", companyId = null)))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched 100505") }
			jsonPath("\$.legalEntities.length()") { value(1) }
		}

		// get company result still responds the old value
		val companiesResultBeforeApprove = mockMvc.get("/company").andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val companiesBeforeApprove : List<Company> = objectMapper.readValue(companiesResultBeforeApprove.response.contentAsString)
		assertThat(companiesBeforeApprove)
			.filteredOn("name", "Third company")
			.isNotEmpty

		// get draft
		val companyAsDraftResult = mockMvc.get("/company/${companyId}/draft/${draftIds[0]}").andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched 100505") }
			jsonPath("\$.legalEntities.length()") { value(1) }
			jsonPath("\$.legalEntities[0].name") { value("Third company patched 100505") }
			jsonPath("\$.legalEntities[0].country") { value("Land of Freedom") }
			jsonPath("\$.legalEntities[0].companyId") { value(companyId.toString()) }
		}.andReturn()

		// submit a draft
		mockMvc.put("/company/$companyId/draft/${draftIds[0]}/approve"){
			this.content = objectMapper.writeValueAsString(CompaniesController.ApproveDraft("Approving 100505"))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched 100505") }
		}

		// get company result responds the new value
		val companiesResultAfterApprove = mockMvc.get("/company").andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val companiesAfterApprove : List<Company> = objectMapper.readValue(companiesResultAfterApprove.response.contentAsString)
		assertThat(companiesAfterApprove)
			.filteredOn("name", "Third company patched 100505")
			.isNotEmpty
	}

	@Test
	fun `approving a company with conflict works`() {
		val userId = UUID.randomUUID()

		// add a company
		val companyResult = mockMvc.post("/company"){
			this.content = objectMapper.writeValueAsString(Company(name = "Third company"))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company") }
		}.andReturn()
		val company: Company = objectMapper.readValue(companyResult.response.contentAsString)
		val companyId = company.identifier

		// create a draft 1
		val draft1Result = mockMvc.post("/company/$companyId/draft"){
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val draft1Response: CompaniesController.DraftResponse = objectMapper.readValue(draft1Result.response.contentAsString)

		// patch a draft 1
		mockMvc.put("/company/$companyId/draft/${draft1Response.draftId}"){
			this.content = objectMapper.writeValueAsString(CompaniesController.EditDraft("Patching in draft 1 100505", Company(name = "Third company patched in draft 1 100505")))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched in draft 1 100505") }
		}

		// create a draft 2
		val draft2Result = mockMvc.post("/company/$companyId/draft"){
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val draft2Response: CompaniesController.DraftResponse = objectMapper.readValue(draft2Result.response.contentAsString)

		// patch a draft 2
		mockMvc.put("/company/$companyId/draft/${draft2Response.draftId}"){
			this.content = objectMapper.writeValueAsString(CompaniesController.EditDraft("Patching in draft 2 555-555", Company(name = "Third company patched in draft 2 555-555")))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched in draft 2 555-555") }
		}

		// submit a draft 1
		mockMvc.put("/company/$companyId/draft/${draft1Response.draftId}/approve"){
			this.header(USER_ID_HEADER, userId)
			this.content = objectMapper.writeValueAsString(CompaniesController.ApproveDraft("Approving in draft 1 100505"))
			this.contentType = MediaType.APPLICATION_JSON
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched in draft 1 100505") }
		}

		// submit a draft 2
		mockMvc.put("/company/$companyId/draft/${draft2Response.draftId}/approve"){
			this.header(USER_ID_HEADER, userId)
			this.content = objectMapper.writeValueAsString(CompaniesController.ApproveDraft("Approving in draft 2 555-555"))
			this.contentType = MediaType.APPLICATION_JSON
		}.andExpect {
			this.status { this.isConflict() }
		}


		// get company result responds the new value from the draft 1
		val companiesResultAfterApprove = mockMvc.get("/company").andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val companiesAfterApprove : List<Company> = objectMapper.readValue(companiesResultAfterApprove.response.contentAsString)
		assertThat(companiesAfterApprove)
			.filteredOn("name", "Third company patched in draft 1 100505")
			.isNotEmpty
	}

	@Test
	fun `getting draft history works`() {
		val userId = UUID.randomUUID()

		// add a company
		val companyResult = mockMvc.post("/company"){
			this.content = objectMapper.writeValueAsString(Company(name = "Third company"))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company") }
		}.andReturn()
		val company: Company = objectMapper.readValue(companyResult.response.contentAsString)
		val companyId = company.identifier

		// create a draft 1
		val draft1Result = mockMvc.post("/company/$companyId/draft"){
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val draft1Response: CompaniesController.DraftResponse = objectMapper.readValue(draft1Result.response.contentAsString)

		// patch a draft 1
		mockMvc.put("/company/$companyId/draft/${draft1Response.draftId}"){
			this.content = objectMapper.writeValueAsString(CompaniesController.EditDraft("First patching in draft 1", Company(name = "Third company patched in draft 1 100505")))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched in draft 1 100505") }
		}

		// patch a draft 1 again
		mockMvc.put("/company/$companyId/draft/${draft1Response.draftId}"){
			this.content = objectMapper.writeValueAsString(CompaniesController.EditDraft("The second patching in draft 1", Company(name = "Third company patched in draft 1 100600")))
			this.contentType = MediaType.APPLICATION_JSON
			this.header(USER_ID_HEADER, userId)
		}.andExpect {
			this.status { this.is2xxSuccessful() }
			jsonPath("\$.name") { value("Third company patched in draft 1 100600") }
		}

		val draftHistoryResult = mockMvc.get("/company/$companyId/draft/${draft1Response.draftId}/history")
		.andExpect {
			this.status { this.is2xxSuccessful() }
		}.andReturn()
		val histories: List<CompaniesController.HistoryResponse> = objectMapper.readValue(draftHistoryResult.response.contentAsString)
		assertThat(histories)
			.hasSize(2)
			.extracting("message")
			.containsExactly(
				"First patching in draft 1",
				"The second patching in draft 1"
			)
	}

}
