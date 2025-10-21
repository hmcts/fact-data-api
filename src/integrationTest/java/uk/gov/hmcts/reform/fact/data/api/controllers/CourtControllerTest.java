package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

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

@WebMvcTest(CourtController.class)
class CourtControllerTest {

    private static final UUID COURT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID REGION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID UNKNOWN_COURT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtService courtService;

    @Test
    @DisplayName("GET /courts/{courtId}/v1 returns court details")
    void getCourtByIdReturnsCourt() throws Exception {
        Court court = buildCourt(COURT_ID);

        when(courtService.getCourtById(COURT_ID)).thenReturn(court);

        mockMvc.perform(get("/courts/{courtId}/v1", COURT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(COURT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Test Court"))
            .andExpect(jsonPath("$.regionId").value(REGION_ID.toString()));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1 returns 404 when court missing")
    void getCourtByIdReturnsNotFound() throws Exception {
        when(courtService.getCourtById(UNKNOWN_COURT_ID)).thenThrow(new NotFoundException("Court not found"));

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
}
