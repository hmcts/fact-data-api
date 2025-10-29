package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtContactDetailsService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtContactDetailsController.class)
class CourtContactDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtContactDetailsService courtContactDetailsService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID contactId = UUID.fromString("223e4567-e89b-12d3-a456-426614174111");

    private CourtContactDetails buildContactDetail() {
        return CourtContactDetails.builder()
            .id(contactId)
            .courtId(courtId)
            .courtContactDescriptionId(UUID.fromString("323e4567-e89b-12d3-a456-426614174222"))
            .explanation("General enquiries")
            .explanationCy("Ymholiadau cyffredinol")
            .email("info@test.com")
            .phoneNumber("01234567890")
            .build();
    }

    @Test
    @DisplayName("GET /courts/{courtId}/contact-details returns contacts successfully")
    void getContactDetailsReturnsSuccessfully() throws Exception {
        when(courtContactDetailsService.getContactDetails(courtId))
            .thenReturn(List.of(buildContactDetail()));

        mockMvc.perform(get("/courts/{courtId}/contact-details", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(contactId.toString()))
            .andExpect(jsonPath("$[0].email").value("info@test.com"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/contact-details returns 404 when court not found")
    void getContactDetailsReturnsNotFoundForUnknownCourt() throws Exception {
        when(courtContactDetailsService.getContactDetails(courtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/contact-details", courtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/contact-details returns 400 for invalid court ID")
    void getContactDetailsInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/contact-details", "invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/contact-details/{contactId} returns contact successfully")
    void getContactDetailReturnsSuccessfully() throws Exception {
        when(courtContactDetailsService.getContactDetail(courtId, contactId))
            .thenReturn(buildContactDetail());

        mockMvc.perform(get("/courts/{courtId}/contact-details/{contactId}", courtId, contactId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(contactId.toString()))
            .andExpect(jsonPath("$.explanation").value("General enquiries"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/contact-details/{contactId} returns 404 when not found")
    void getContactDetailReturnsNotFound() throws Exception {
        when(courtContactDetailsService.getContactDetail(courtId, contactId))
            .thenThrow(new NotFoundException("Contact not found"));

        mockMvc.perform(get("/courts/{courtId}/contact-details/{contactId}", courtId, contactId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/contact-details/{contactId} returns 400 for invalid ID")
    void getContactDetailInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/contact-details/{contactId}", "invalid", contactId))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/contact-details creates contact successfully")
    void createContactDetailReturnsCreated() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setId(null);

        when(courtContactDetailsService.createContactDetail(any(UUID.class), any(CourtContactDetails.class)))
            .thenReturn(buildContactDetail());

        mockMvc.perform(post("/courts/{courtId}/contact-details", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("info@test.com"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/contact-details returns 404 when court not found")
    void createContactDetailReturnsNotFound() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setId(null);

        when(courtContactDetailsService.createContactDetail(any(UUID.class), any(CourtContactDetails.class)))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/courts/{courtId}/contact-details", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/contact-details returns 400 for invalid email")
    void createContactDetailInvalidEmail() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setId(null);
        request.setEmail("invalid-email");

        mockMvc.perform(post("/courts/{courtId}/contact-details", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/contact-details returns 400 when explanation too long")
    void createContactDetailExplanationTooLong() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setId(null);
        request.setExplanation("a".repeat(251));

        mockMvc.perform(post("/courts/{courtId}/contact-details", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/contact-details returns 400 when explanation contains invalid characters")
    void createContactDetailExplanationInvalidCharacters() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setId(null);
        request.setExplanation("Invalid explanation!");

        mockMvc.perform(post("/courts/{courtId}/contact-details", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/contact-details/{contactId} updates contact successfully")
    void updateContactDetailReturnsOk() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setExplanation("Updated explanation");

        when(courtContactDetailsService.updateContactDetail(
            any(UUID.class),
            any(UUID.class),
            any(CourtContactDetails.class)
        )).thenReturn(request);

        mockMvc.perform(put("/courts/{courtId}/contact-details/{contactId}", courtId, contactId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.explanation").value("Updated explanation"));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/contact-details/{contactId} returns 404 when contact not found")
    void updateContactDetailReturnsNotFound() throws Exception {
        CourtContactDetails request = buildContactDetail();

        when(courtContactDetailsService.updateContactDetail(
            any(UUID.class),
            any(UUID.class),
            any(CourtContactDetails.class)
        )).thenThrow(new NotFoundException("Contact not found"));

        mockMvc.perform(put("/courts/{courtId}/contact-details/{contactId}", courtId, contactId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/contact-details/{contactId} returns 400 for invalid phone number")
    void updateContactDetailInvalidPhone() throws Exception {
        CourtContactDetails request = buildContactDetail();
        request.setPhoneNumber("123");

        mockMvc.perform(put("/courts/{courtId}/contact-details/{contactId}", courtId, contactId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/contact-details/{contactId} deletes contact successfully")
    void deleteContactDetailReturnsNoContent() throws Exception {
        doNothing().when(courtContactDetailsService).deleteContactDetail(courtId, contactId);

        mockMvc.perform(delete("/courts/{courtId}/contact-details/{contactId}", courtId, contactId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/contact-details/{contactId} returns 404 when contact not found")
    void deleteContactDetailReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Contact not found"))
            .when(courtContactDetailsService).deleteContactDetail(courtId, contactId);

        mockMvc.perform(delete("/courts/{courtId}/contact-details/{contactId}", courtId, contactId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/contact-details/{contactId} returns 400 for invalid UUID")
    void deleteContactDetailInvalidUUID() throws Exception {
        mockMvc.perform(delete("/courts/{courtId}/contact-details/{contactId}", "invalid", contactId))
            .andExpect(status().isBadRequest());
    }
}
