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
            .hearingEnhancementEquipment("Equipment")
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
            .hearingEnhancementEquipment("Equipment")
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

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for null required fields")
    void postAccessibilityOptionsNullRequiredFields() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .accessibleEntrance(null)
            .court(null)
            .accessibleParking(false)
            .hearingEnhancementEquipment("Equipment")
            .lift(true)
            .quietRoom(true)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for oversized text fields")
    void postAccessibilityOptionsOversizedFields() throws Exception {
        String oversizedText = "a".repeat(1001);
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .lift(true)
            .quietRoom(true)
            .hearingEnhancementEquipment(oversizedText)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for invalid phone number format")
    void postAccessibilityOptionsInvalidPhoneFormat() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .hearingEnhancementEquipment("Equipment")
            .lift(true)
            .quietRoom(true)
            .accessibleParkingPhoneNumber("invalid-phone")
            .accessibleEntrancePhoneNumber("invalid-phone")
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for oversized phone numbers")
    void postAccessibilityOptionsOversizedPhoneNumber() throws Exception {
        String oversizedPhone = "1".repeat(51);
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .hearingEnhancementEquipment("Equipment")
            .lift(true)
            .quietRoom(true)
            .accessibleParkingPhoneNumber(oversizedPhone)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for invalid lift door width")
    void postAccessibilityOptionsInvalidLiftDoorWidth() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .hearingEnhancementEquipment("Equipment")
            .lift(true)
            .quietRoom(true)
            .liftDoorWidth(1001)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for invalid lift weight limits")
    void postAccessibilityOptionsInvalidLiftWeightLimits() throws Exception {
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .hearingEnhancementEquipment("Equipment")
            .lift(true)
            .quietRoom(true)
            .liftDoorLimit(10001)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 400 for oversized toilet descriptions")
    void postAccessibilityOptionsOversizedToiletDescription() throws Exception {
        String oversizedDescription = "a".repeat(256);
        CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .accessibleEntrance(true)
            .accessibleParking(false)
            .hearingEnhancementEquipment("Equipment")
            .lift(true)
            .quietRoom(true)
            .accessibleToiletDescription(oversizedDescription)
            .accessibleToiletDescriptionCy(oversizedDescription)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/accessibility-options", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accessibilityOptions)))
            .andExpect(status().isBadRequest());
    }
}
