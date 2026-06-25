package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditActionType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Feature;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Feature("Audit Controller")
@DisplayName("Audit Controller")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    CourtRepository courtRepository;

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    ServiceCentreRepository serviceCentreRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuditUserContext auditUserContext;

    @Autowired
    private MockMvc mvc;

    List<Region> regions;

    @BeforeEach
    void setUp() {
        auditUserContext.suppressAudit();
        // we're going to need these
        regions = regionRepository.findAll();
        // and clear these down
        courtRepository.deleteAll();
        serviceCentreRepository.deleteAll();
        auditRepository.deleteAll();
        auditUserContext.clear();

        User user = userRepository.save(User.builder()
            .email("audit-controller-test-" + UUID.randomUUID() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.ADMIN)
            .build());
        auditUserContext.setUserId(user.getId());
    }

    @Test
    @DisplayName("GET /audits/v1 returns paginated list")
    void getFilteredAndPaginatedAuditsReturnsResults() throws Exception {
        createTestCourts(13);

        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalPages").value(3))
            .andExpect(jsonPath("$.page.totalElements").value(13));

        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.totalPages").value(3))
            .andExpect(jsonPath("$.page.totalElements").value(13));

        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "2")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.page.number").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(3))
            .andExpect(jsonPath("$.page.totalElements").value(13));

    }

    @Test
    @DisplayName("GET /audits/v1 returns correctly filtered results")
    void getFilteredAndPaginatedAuditsReturnsFilteredResults() throws Exception {

        //create
        Court court1 = createTestCourts(1).getFirst();
        // change
        for (int i = 0; i < 5; i++) {
            court1.setName("Court " + RandomStringUtils.insecure().next(10, true, false));
            courtRepository.save(court1);
        }
        // change
        court1.setOpen(!Optional.ofNullable(court1.getOpen()).orElse(false));
        courtRepository.save(court1);
        // (total 7)

        // create
        Court court2 = createTestCourts(1).getFirst();
        // change
        court2.setName("Court two");
        courtRepository.save(court2);
        // change
        court2.setOpen(!Optional.ofNullable(court2.getOpen()).orElse(false));
        courtRepository.save(court2);
        // (total 3)

        // court1
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("fromDate", LocalDate.now().toString())
                        .param("courtId", court1.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(7))
            .andExpect(jsonPath("$.content[0]").exists())
            .andExpect(jsonPath("$.content[*].subjectId").value(allElementsEqual(court1.getId().toString())))
            .andExpect(jsonPath("$.content[*].subjectType").value(allElementsEqual("COURT")))
            .andExpect(jsonPath("$.content[0].court").doesNotExist())
            .andExpect(jsonPath("$.content[0].courtId").doesNotExist())
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(7));

        // court2
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("fromDate", LocalDate.now().toString())
                        .param("courtId", court2.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0]").exists())
            .andExpect(jsonPath("$.content[*].subjectId").value(allElementsEqual(court2.getId().toString())))
            .andExpect(jsonPath("$.content[*].subjectType").value(allElementsEqual("COURT")))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("GET /audits/v1 returns results filtered by serviceCentreId")
    void getFilteredAndPaginatedAuditsReturnsServiceCentreFilteredResults() throws Exception {

        ServiceCentre serviceCentre1 = createTestServiceCentre();
        for (int i = 0; i < 2; i++) {
            serviceCentre1.setOpen(!Optional.ofNullable(serviceCentre1.getOpen()).orElse(false));
            serviceCentreRepository.save(serviceCentre1);
        }

        ServiceCentre serviceCentre2 = createTestServiceCentre();
        serviceCentre2.setOpen(!Optional.ofNullable(serviceCentre2.getOpen()).orElse(false));
        serviceCentreRepository.save(serviceCentre2);

        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("fromDate", LocalDate.now().toString())
                        .param("serviceCentreId", serviceCentre1.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0]").exists())
            .andExpect(jsonPath("$.content[*].subjectId").value(allElementsEqual(serviceCentre1.getId().toString())))
            .andExpect(jsonPath("$.content[*].subjectType").value(allElementsEqual("SERVICE_CENTRE")))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(3));
    }


    @Test
    @DisplayName("GET /audits/v1 returns correct action types")
    void getFilteredAndPaginatedAuditsReturnCorrectActionTypes() throws Exception {

        //create
        Court court1 = createTestCourts(1).getFirst();
        // change
        for (int i = 0; i < 3; i++) {
            court1.setName("Court " + RandomStringUtils.insecure().next(10, true, false));
            courtRepository.save(court1);
        }
        // change
        court1.setOpen(!Optional.ofNullable(court1.getOpen()).orElse(false));
        courtRepository.save(court1);
        courtRepository.delete(court1);
        // (total 6)

        // court1
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("fromDate", LocalDate.now().toString())
                        .param("courtId", court1.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0]").exists())
            .andExpect(jsonPath("$.content[*].actionType").value(IsEqual.equalTo(List.of(
                AuditActionType.DELETE.name(),
                AuditActionType.UPDATE.name(),
                AuditActionType.UPDATE.name(),
                AuditActionType.UPDATE.name(),
                AuditActionType.UPDATE.name(),
                AuditActionType.INSERT.name()
            ))))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(6))
            .andExpect(jsonPath("$.page.totalElements").value(6));
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when fromDate missing")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForMissingFromDate() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when date range is invalid")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForInvalidDateRange() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().minusDays(2).toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when email is invalid")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForInvalidEmail() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString())
                        .param("email", "&^*&^*"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when courtId is invalid")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForInvalidCourtId() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString())
                        .param("courtId", "this-is-not-a-valid-court-id"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when serviceCentreId is invalid")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForInvalidServiceCentreId() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString())
                        .param("serviceCentreId", "this-is-not-a-valid-service-centre-id"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when courtId and serviceCentreId are both provided")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForBothSubjectIds() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString())
                        .param("courtId", UUID.randomUUID().toString())
                        .param("serviceCentreId", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when page size is invalid")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForInvalidPageSize() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "-3")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/v1 returns returns 400 when page number is invalid")
    void getFilteredAndPaginatedAuditsReturnsBadRequestForInvalidPageNumber() throws Exception {
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "-3")
                        .param("pageSize", "100")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /audits/v1 returns 204")
    void deleteAuditsRemovesExpiredAuditsAndReturns204() throws Exception {
        mvc.perform(delete("/audits/v1")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /audits/subjectoptions/v1 returns subject options map")
    void getSubjectNameAndIdMapReturnsResults() throws Exception {
        // Seed at least one court so COURT options are present
        createTestCourts(1);
        createTestServiceCentre();

        mvc.perform(get("/audits/subjectoptions/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.COURT").isArray())
            .andExpect(jsonPath("$.SERVICE_CENTRE").isArray())
            .andExpect(jsonPath("$.COURT[0].name").exists())
            .andExpect(jsonPath("$.COURT[0].id").exists())
            .andExpect(jsonPath("$.SERVICE_CENTRE[0].name").exists())
            .andExpect(jsonPath("$.SERVICE_CENTRE[0].id").exists());
    }

    @Test
    @DisplayName("GET /audits/{auditId}/v1 returns audit when id is valid")
    void getAuditByIdReturnsAuditWhenIdIsValid() throws Exception {
        Court court = createTestCourts(1).getFirst();

        String auditsResponse = mvc.perform(get("/audits/v1")
                                                .param("pageNumber", "0")
                                                .param("pageSize", "10")
                                                .param("fromDate", LocalDate.now().toString())
                                                .param("courtId", court.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String auditId = JsonPath.read(auditsResponse, "$.content[0].id");

        mvc.perform(get("/audits/" + auditId + "/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(auditId))
            .andExpect(jsonPath("$.subjectId").value(court.getCourtId().toString()))
            .andExpect(jsonPath("$.actionType").exists())
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("GET /audits/{auditId}/v1 returns 400 when auditId is not a UUID")
    void getAuditByIdReturnsBadRequestWhenAuditIdIsInvalid() throws Exception {
        mvc.perform(get("/audits/not-a-valid-uuid/v1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /audits/{auditId}/v1 returns 404 when audit does not exist")
    void getAuditByIdReturnsNotFoundWhenAuditDoesNotExist() throws Exception {
        mvc.perform(get("/audits/" + UUID.randomUUID() + "/v1"))
            .andExpect(status().isNotFound());
    }

    private List<Court> createTestCourts(int num) {
        List<Court> results = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            Collections.shuffle(regions);
            Court court = buildCourt(
                regions.getFirst().getId(),
                "Court " + RandomStringUtils.insecure().next(10, true, false)
            );
            results.add(courtRepository.save(court));
        }
        return results;
    }

    private ServiceCentre createTestServiceCentre() {
        return serviceCentreRepository.save(ServiceCentre.builder()
            .name("Service Centre " + RandomStringUtils.insecure().next(10, true, false))
            .open(Boolean.FALSE)
            .build());
    }

    private Court buildCourt(UUID regionId, String name) {
        return Court.builder()
            .name(name)
            .open(Boolean.FALSE)
            .regionId(regionId)
            .build();
    }


    // custom matcher for testing all elements in a list against a single object
    private static Matcher<List<Object>> allElementsEqual(final Object value) {
        return new TypeSafeMatcher<List<Object>>() {
            @Override
            protected boolean matchesSafely(List<Object> item) {
                if (item == null || item.isEmpty()) {
                    return true;
                }
                return item.stream().allMatch(i -> Objects.equals(i, value));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected all elements to equal " + Objects.toString(value));
            }
        };
    }
}
