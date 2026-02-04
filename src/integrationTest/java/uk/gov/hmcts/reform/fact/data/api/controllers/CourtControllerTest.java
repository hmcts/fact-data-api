package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Court Controller")
@DisplayName("Court Controller")
@WebMvcTest(CourtController.class)
class CourtControllerTest {

    private static final UUID COURT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID REGION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID UNKNOWN_COURT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String COURT_SLUG = "test-court";
    private static final String UNKNOWN_COURT_SLUG = "missing-court";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtService courtService;

    @Test
    @DisplayName("GET /courts/{courtId}/v1 returns court details")
    void getCourtByIdReturnsCourt() throws Exception {
        CourtDetails courtDetails = buildCourtDetails(COURT_ID, "Test Court");

        when(courtService.getCourtDetailsById(COURT_ID)).thenReturn(courtDetails);

        mockMvc.perform(get("/courts/{courtId}/v1", COURT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Test Court"))
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1 returns 404 when court missing")
    void getCourtDetailsByIdReturnsNotFound() throws Exception {
        when(courtService.getCourtDetailsById(UNKNOWN_COURT_ID)).thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1", UNKNOWN_COURT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1 returns 400 for invalid UUID")
    void getCourtByIdReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/slug/{courtSlug}/v1 returns court details")
    void getCourtBySlugReturnsCourt() throws Exception {
        CourtDetails courtDetails = buildCourtDetails(COURT_ID, "Test Court");

        when(courtService.getCourtDetailsBySlug(COURT_SLUG)).thenReturn(courtDetails);

        mockMvc.perform(get("/courts/slug/{courtSlug}/v1", COURT_SLUG))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Test Court"))
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
    }

    @Test
    @DisplayName("GET /courts/slug/{courtSlug}/v1 returns 404 when court missing")
    void getCourtDetailsBySlugReturnsNotFound() throws Exception {
        when(courtService.getCourtDetailsBySlug(UNKNOWN_COURT_SLUG))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/slug/{courtSlug}/v1", UNKNOWN_COURT_SLUG))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/slug/{courtSlug}/v1 returns 400 for invalid slug")
    void getCourtBySlugReturnsBadRequestForInvalidSlug() throws Exception {
        mockMvc.perform(get("/courts/slug/{courtSlug}/v1", "INVALID SLUG"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                ValidationConstants.COURT_SLUG_REGEX_MESSAGE
            )));
    }

    @Test
    @DisplayName("GET /courts/slug/{courtSlug}/v1 returns 400 for slug with brackets")
    void getCourtBySlugReturnsBadRequestForSlugWithBrackets() throws Exception {
        mockMvc.perform(get("/courts/slug/{courtSlug}/v1", "test-court(family)-123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                ValidationConstants.COURT_SLUG_REGEX_MESSAGE
            )));
    }

    @Test
    @DisplayName("GET /courts/slug/{courtSlug}/v1 returns 400 for slug below min length")
    void getCourtBySlugReturnsBadRequestForShortSlug() throws Exception {
        mockMvc.perform(get("/courts/slug/{courtSlug}/v1", "abcd"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                ValidationConstants.COURT_SLUG_LENGTH_MESSAGE
            )));
    }

    @Test
    @DisplayName("GET /courts/slug/{courtSlug}.json returns court details")
    void getCourtBySlugJsonPathReturnsCourt() throws Exception {
        CourtDetails courtDetails = buildCourtDetails(COURT_ID, "Test Court");

        when(courtService.getCourtDetailsBySlug(COURT_SLUG)).thenReturn(courtDetails);

        mockMvc.perform(get("/courts/slug/{courtSlug}.json", COURT_SLUG))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Test Court"))
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
    }

    @Test
    @DisplayName("GET /courts/v1 returns paginated list")
    void getFilteredAndPaginatedCourtsReturnsResults() throws Exception {
        Court court = buildCourt(COURT_ID);
        Page<Court> courts = new PageImpl<>(List.of(court));

        when(courtService.getFilteredAndPaginatedCourts(
            any(Pageable.class),
            anyBoolean(),
            anyString(),
            anyString()
        )).thenReturn(courts);

        mockMvc.perform(get("/courts/v1")
                            .param("pageNumber", "0")
                            .param("pageSize", "25")
                            .param("includeClosed", "true")
                            .param("regionId", REGION_ID.toString())
                            .param("partialCourtName", "Test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.content[0].name").value("Test Court"));
    }

    @Test
    @DisplayName("POST /courts/v1 creates a new court")
    void createCourtReturnsCreated() throws Exception {
        Court request = buildCourt(null);
        Court created = buildCourt(COURT_ID);

        when(courtService.createCourt(any(Court.class))).thenReturn(created);

        mockMvc.perform(post("/courts/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Test Court"));
    }

    @Test
    @DisplayName("POST /courts/v1 returns 404 when dependent data missing")
    void createCourtReturnsNotFound() throws Exception {
        Court request = buildCourt(null);

        when(courtService.createCourt(any(Court.class))).thenThrow(new NotFoundException("Region missing"));

        mockMvc.perform(post("/courts/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 updates court")
    void updateCourtReturnsOk() throws Exception {
        Court request = buildCourt(null);
        Court updated = buildCourt(COURT_ID);

        when(courtService.updateCourt(COURT_ID, request)).thenReturn(updated);

        mockMvc.perform(put("/courts/{courtId}/v1", COURT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Test Court"));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 returns 404 when court missing")
    void updateCourtReturnsNotFound() throws Exception {
        Court request = buildCourt(null);

        when(courtService.updateCourt(UNKNOWN_COURT_ID, request))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(put("/courts/{courtId}/v1", UNKNOWN_COURT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 returns 400 for invalid UUID")
    void updateCourtReturnsBadRequestForInvalidUuid() throws Exception {
        Court request = buildCourt(null);

        mockMvc.perform(put("/courts/{courtId}/v1", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/v1 returns 400 for invalid partial court name")
    void getFilteredCourtsReturnsBadRequestForInvalidPartialName() throws Exception {
        mockMvc.perform(get("/courts/v1")
                            .param("partialCourtName", "Invalid@Name"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/v1 returns 400 for invalid region UUID")
    void getFilteredCourtsReturnsBadRequestForInvalidRegionId() throws Exception {
        mockMvc.perform(get("/courts/v1")
                            .param("regionId", "not-a-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/v1 returns 400 when partial court name exceeds max length")
    void getFilteredCourtsReturnsBadRequestForPartialNameTooLong() throws Exception {
        mockMvc.perform(get("/courts/v1")
                            .param("partialCourtName", "A".repeat(251)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/v1 returns 400 for negative page number")
    void getFilteredCourtsReturnsBadRequestForNegativePageNumber() throws Exception {
        mockMvc.perform(get("/courts/v1")
                            .param("pageNumber", "-1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/v1 returns 400 for invalid name pattern")
    void createCourtReturnsBadRequestForInvalidName() throws Exception {
        Court invalidCourt = buildCourt(null);
        invalidCourt.setName("Name@");

        mockMvc.perform(post("/courts/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCourt)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/v1 returns 400 for invalid slug pattern")
    void createCourtReturnsBadRequestForInvalidSlug() throws Exception {
        Court invalidCourt = buildCourt(null);
        invalidCourt.setSlug("INVALID SLUG");

        mockMvc.perform(post("/courts/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCourt)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 returns 400 for invalid request body")
    void updateCourtReturnsBadRequestForInvalidBody() throws Exception {
        Court invalidCourt = buildCourt(null);
        invalidCourt.setName("Invalid@Name");

        mockMvc.perform(put("/courts/{courtId}/v1", COURT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCourt)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/all/v1 return 200 and the complete list of CourtDetails")
    void getAllCourtsReturns200() throws Exception {
        List<CourtDetails> courtDetailsList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            courtDetailsList.add(buildCourtDetails(
                UUID.randomUUID(),
                String.format("Test Court %s", (char) (i + 0x41))
            ));
        }

        when(courtService.getAllCourtDetails()).thenReturn(courtDetailsList);

        MvcResult result = mockMvc.perform(get("/courts/all/v1"))
            .andExpect(status().isOk())
            .andReturn();

        List<CourtDetails> responseList = List.of(objectMapper.readValue(
            result.getResponse().getContentAsString(),
            CourtDetails[].class
        ));

        Assertions.assertThat(responseList).isEqualTo(courtDetailsList);
    }


    private Court buildCourt(UUID id) {
        return Court.builder()
            .id(id)
            .name("Test Court")
            .slug("test-court")
            .open(Boolean.TRUE)
            .warningNotice("Notice")
            .regionId(REGION_ID)
            .isServiceCentre(Boolean.TRUE)
            .openOnCath(Boolean.TRUE)
            .mrdId("MRD123")
            .build();
    }

    private CourtDetails buildCourtDetails(UUID id, String name) {
        return CourtDetails.builder()
            .id(id)
            .name(name)
            .slug(courtService.toSlugFormat(name))
            .open(Boolean.TRUE)
            .warningNotice("Notice")
            .regionId(REGION_ID)
            .isServiceCentre(Boolean.TRUE)
            .openOnCath(Boolean.TRUE)
            .mrdId("MRD123")
            .build();
    }
}
