package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditActionType;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.qameta.allure.Feature;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@Feature("Audit Controller")
@DisplayName("Audit Controller")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.config.name=application-test"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    CourtRepository courtRepository;

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    private MockMvc mvc;

    List<Region> regions;

    @BeforeEach
    void setUp() {
        // we're going to need these
        regions = regionRepository.findAll();
        // and clear these down
        courtRepository.deleteAll();
        auditRepository.deleteAll();
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
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false))
            .andExpect(jsonPath("$.numberOfElements").value(5))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.totalElements").value(13));

        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(false))
            .andExpect(jsonPath("$.numberOfElements").value(5))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.totalElements").value(13));

        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "2")
                        .param("pageSize", "5")
                        .param("fromDate", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.numberOfElements").value(3))
            .andExpect(jsonPath("$.number").value(2))
            .andExpect(jsonPath("$.totalElements").value(13));

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
            .andExpect(jsonPath("$.content[0]").exists())
            .andExpect(jsonPath("$.content[*].court.id").value(allElementsEqual(court1.getCourtId().toString())))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.numberOfElements").value(7))
            .andExpect(jsonPath("$.totalElements").value(7));


        // court2
        mvc.perform(get("/audits/v1")
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("fromDate", LocalDate.now().toString())
                        .param("courtId", court2.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0]").exists())
            .andExpect(jsonPath("$.content[*].court.id").value(allElementsEqual(court2.getCourtId().toString())))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.numberOfElements").value(3))
            .andExpect(jsonPath("$.totalElements").value(3));
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
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.numberOfElements").value(6))
            .andExpect(jsonPath("$.totalElements").value(6));
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

    private Court buildCourt(UUID regionId, String name) {
        return Court.builder()
            .name(name)
            .regionId(regionId)
            .isServiceCentre(Boolean.FALSE)
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
