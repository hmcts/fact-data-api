package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreContactDetailsService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Service Centre Contact Details Controller")
@DisplayName("Service Centre Contact Details Controller")
@WebMvcTest(ServiceCentreContactDetailsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ServiceCentreContactDetailsControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID CONTACT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceCentreContactDetailsService serviceCentreContactDetailsService;

    @Test
    void getContactDetailsReturnsContactDetails() throws Exception {
        ServiceCentreContactDetails contactDetails = buildContactDetails();
        when(serviceCentreContactDetailsService.getContactDetails(SERVICE_CENTRE_ID))
            .thenReturn(List.of(contactDetails));

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/contact-details", SERVICE_CENTRE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(CONTACT_ID.toString()));
    }

    @Test
    void getContactDetailReturnsContactDetail() throws Exception {
        ServiceCentreContactDetails contactDetails = buildContactDetails();
        when(serviceCentreContactDetailsService.getContactDetail(SERVICE_CENTRE_ID, CONTACT_ID))
            .thenReturn(contactDetails);

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/contact-details/{contactId}",
                            SERVICE_CENTRE_ID,
                            CONTACT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(CONTACT_ID.toString()));
    }

    @Test
    void getContactDetailReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/contact-details/{contactId}",
                            SERVICE_CENTRE_ID,
                            "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postContactDetailCreatesContactDetail() throws Exception {
        ServiceCentreContactDetails contactDetails = buildContactDetails();
        when(serviceCentreContactDetailsService.createContactDetail(
            eq(SERVICE_CENTRE_ID),
            any(ServiceCentreContactDetails.class)
        )).thenReturn(contactDetails);

        mockMvc.perform(post("/service-centres/{serviceCentreId}/v1/contact-details", SERVICE_CENTRE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(contactDetails)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(CONTACT_ID.toString()));
    }

    @Test
    void putContactDetailUpdatesContactDetail() throws Exception {
        ServiceCentreContactDetails contactDetails = buildContactDetails();
        when(serviceCentreContactDetailsService.updateContactDetail(
            eq(SERVICE_CENTRE_ID),
            eq(CONTACT_ID),
            any(ServiceCentreContactDetails.class)
        )).thenReturn(contactDetails);

        mockMvc.perform(put("/service-centres/{serviceCentreId}/v1/contact-details/{contactId}",
                            SERVICE_CENTRE_ID,
                            CONTACT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(contactDetails)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(CONTACT_ID.toString()));
    }

    @Test
    void deleteContactDetailReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/service-centres/{serviceCentreId}/v1/contact-details/{contactId}",
                               SERVICE_CENTRE_ID,
                               CONTACT_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteContactDetailReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Missing"))
            .when(serviceCentreContactDetailsService).deleteContactDetail(SERVICE_CENTRE_ID, CONTACT_ID);

        mockMvc.perform(delete("/service-centres/{serviceCentreId}/v1/contact-details/{contactId}",
                               SERVICE_CENTRE_ID,
                               CONTACT_ID))
            .andExpect(status().isNotFound());
    }

    private ServiceCentreContactDetails buildContactDetails() {
        return ServiceCentreContactDetails.builder()
            .id(CONTACT_ID)
            .serviceCentreId(SERVICE_CENTRE_ID)
            .explanation("General enquiries")
            .email("test@example.com")
            .phoneNumber("020 123 456")
            .build();
    }
}
