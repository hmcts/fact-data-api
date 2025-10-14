package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtTranslationService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtTranslationController.class)
class CourtTranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtTranslationService courtTranslationService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services returns translation successfully")
    void getTranslationReturnsSuccessfully() throws Exception {
        CourtTranslation translation = CourtTranslation.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .email("test@court.com")
            .phoneNumber("01234567890")
            .build();

        when(courtTranslationService.getTranslationByCourtId(courtId)).thenReturn(translation);

        mockMvc.perform(get("/courts/{courtId}/v1/translation-services", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@court.com"))
            .andExpect(jsonPath("$.phoneNumber").value("01234567890"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services returns 404 if court does not exist")
    void getCourtNonExistentReturnsNotFound() throws Exception {
        when(courtTranslationService.getTranslationByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/translation-services", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services returns 204 if translation does not exist")
    void getTranslationNonExistentCourtReturnsNoContent() throws Exception {
        when(courtTranslationService.getTranslationByCourtId(courtId))
            .thenThrow(new TranslationNotFoundException("Translation not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/translation-services", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services returns 400 for invalid UUID")
    void getTranslationInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/translation-services", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services creates translation successfully")
    void postTranslationCreatesSuccessfully() throws Exception {
        CourtTranslation translation = CourtTranslation.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .email("test@court.com")
            .phoneNumber("01234567890")
            .build();

        when(courtTranslationService.setTranslation(any(UUID.class),
                                                    any(CourtTranslation.class))).thenReturn(translation);

        mockMvc.perform(post("/courts/{courtId}/v1/translation-services", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(translation)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@court.com"))
            .andExpect(jsonPath("$.phoneNumber").value("01234567890"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services returns 404 if court does not exist")
    void postTranslationNonExistentCourtReturnsNotFound() throws Exception {
        CourtTranslation translation = CourtTranslation.builder()
            .id(nonExistentCourtId)
            .courtId(nonExistentCourtId)
            .court(null)
            .email("test@court.com")
            .phoneNumber("01234567890")
            .build();

        when(courtTranslationService.setTranslation(any(UUID.class), any(CourtTranslation.class)))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/courts/{courtId}/v1/translation-services", nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(translation)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services returns 400 for invalid email")
    void postTranslationInvalidEmailReturnsBadRequest() throws Exception {
        CourtTranslation invalid = CourtTranslation.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .email("invalid-email")
            .phoneNumber("01234567890")
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/translation-services", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services returns 400 for invalid phone number")
    void postTranslationInvalidPhoneReturnsBadRequest() throws Exception {
        CourtTranslation invalid = CourtTranslation.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .email("test@court.com")
            .phoneNumber("123")
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/translation-services", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services returns 400 for invalid UUID")
    void postTranslationInvalidUUID() throws Exception {
        CourtTranslation translation = CourtTranslation.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .email("test@court.com")
            .phoneNumber("01234567890")
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/translation-services", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(translation)))
            .andExpect(status().isBadRequest());
    }
}
