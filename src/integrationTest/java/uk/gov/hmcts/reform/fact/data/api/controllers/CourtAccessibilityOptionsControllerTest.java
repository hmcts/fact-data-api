package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAccessibilityOptionsService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Court Accessibility Options Controller")
@DisplayName("Court Accessibility Options Controller")
@WebMvcTest(CourtAccessibilityOptionsController.class)
class CourtAccessibilityOptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtAccessibilityOptionsService courtAccessibilityOptionsService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("GET /courts/{courtId}/v1/accessibility-options returns Accessibility Options successfully")
    void getAccessibilityOptionsReturnsSuccessfully() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .lift(true)
            .quietRoom(true)
            .build();

        when(courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(courtId))
            .thenReturn(accessibilityOptions);

        mockMvc.perform(get("/courts/{courtId}/v1/accessibility-options", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessibleEntrance").value(true))
            .andExpect(jsonPath("$.accessibleParking").value(false))
            .andExpect(jsonPath("$.lift").value(true))
            .andExpect(jsonPath("$.quietRoom").value(true));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/accessibility-options returns 404 if court does not exist")
    void getCourtNonExistentReturnsNotFound() throws Exception {
        when(courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/accessibility-options", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/accessibility-options returns 204 if Accessibility Options does not exist")
    void getAccessibilityOptionsNonExistentCourtReturnsNoContent() throws Exception {
        when(courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(courtId))
            .thenThrow(new NotFoundException("Accessibility Options not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/accessibility-options", courtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/accessibility-options returns 400 for invalid UUID")
    void getAccessibilityOptionsInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/accessibility-options", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options creates Accessibility Options successfully")
    void postAccessibilityOptionsCreatesSuccessfully() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .lift(true)
            .quietRoom(true)
            .build();

        when(
            courtAccessibilityOptionsService
                .setAccessibilityOptions(any(UUID.class), any(CourtAccessibilityOptions.class)))
                .thenReturn(accessibilityOptions);

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessibleEntrance").value(true))
            .andExpect(jsonPath("$.accessibleParking").value(false))
            .andExpect(jsonPath("$.lift").value(true))
            .andExpect(jsonPath("$.quietRoom").value(true));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 404 if court does not exist")
    void postAccessibilityOptionsNonExistentCourtReturnsNotFound() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(nonExistentCourtId)
            .courtId(nonExistentCourtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .lift(true)
            .quietRoom(true)
            .build();

        when(courtAccessibilityOptionsService
                 .setAccessibilityOptions(any(UUID.class), any(CourtAccessibilityOptions.class)))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for invalid UUID")
    void postAccessibilityOptionsInvalidUUID() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .lift(true)
            .quietRoom(true)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }
}
