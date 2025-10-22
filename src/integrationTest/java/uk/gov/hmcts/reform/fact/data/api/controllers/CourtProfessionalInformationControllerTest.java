package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.ProfessionalInformationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtProfessionalInformationService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtProfessionalInformationController.class)
@Import(CourtProfessionalInformationControllerTest.TestConfig.class)
class CourtProfessionalInformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourtProfessionalInformationService professionalInformationService;

    @AfterEach
    void tearDown() {
        Mockito.reset(professionalInformationService);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CourtProfessionalInformationService professionalInformationService() {
            return Mockito.mock(CourtProfessionalInformationService.class);
        }
    }

    @Test
    void shouldReturnProfessionalInformationForCourt() throws Exception {
        UUID courtId = UUID.randomUUID();
        CourtProfessionalInformation professionalInformation = new CourtProfessionalInformation();
        professionalInformation.setId(UUID.randomUUID());
        professionalInformation.setCourtId(courtId);
        professionalInformation.setInterviewRooms(Boolean.TRUE);
        professionalInformation.setInterviewRoomCount(3);
        professionalInformation.setInterviewPhoneNumber("01234 567890");
        professionalInformation.setVideoHearings(Boolean.TRUE);
        professionalInformation.setCommonPlatform(Boolean.FALSE);
        professionalInformation.setAccessScheme(Boolean.TRUE);

        when(professionalInformationService.getProfessionalInformation(courtId)).thenReturn(professionalInformation);

        mockMvc.perform(get("/courts/{courtId}/professional-information", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(professionalInformation.getId().toString()))
            .andExpect(jsonPath("$.courtId").value(courtId.toString()))
            .andExpect(jsonPath("$.interviewRooms").value(true))
            .andExpect(jsonPath("$.interviewRoomCount").value(3))
            .andExpect(jsonPath("$.interviewPhoneNumber").value("01234 567890"))
            .andExpect(jsonPath("$.videoHearings").value(true))
            .andExpect(jsonPath("$.commonPlatform").value(false))
            .andExpect(jsonPath("$.accessScheme").value(true));
    }

    @Test
    void shouldReturnNoContentWhenProfessionalInformationMissing() throws Exception {
        UUID courtId = UUID.randomUUID();
        when(professionalInformationService.getProfessionalInformation(courtId))
            .thenThrow(new ProfessionalInformationNotFoundException("missing"));

        mockMvc.perform(get("/courts/{courtId}/professional-information", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldCreateOrUpdateProfessionalInformation() throws Exception {
        UUID courtId = UUID.randomUUID();
        CourtProfessionalInformation request = new CourtProfessionalInformation();
        request.setCourtId(courtId);
        request.setInterviewRooms(Boolean.TRUE);
        request.setInterviewRoomCount(4);
        request.setInterviewPhoneNumber("01234 567890");
        request.setVideoHearings(Boolean.FALSE);
        request.setCommonPlatform(Boolean.TRUE);
        request.setAccessScheme(Boolean.FALSE);

        CourtProfessionalInformation response = new CourtProfessionalInformation();
        response.setId(UUID.randomUUID());
        response.setCourtId(courtId);
        response.setInterviewRooms(request.getInterviewRooms());
        response.setInterviewRoomCount(request.getInterviewRoomCount());
        response.setInterviewPhoneNumber(request.getInterviewPhoneNumber());
        response.setVideoHearings(request.getVideoHearings());
        response.setCommonPlatform(request.getCommonPlatform());
        response.setAccessScheme(request.getAccessScheme());

        when(professionalInformationService.setProfessionalInformation(eq(courtId), any()))
            .thenReturn(response);

        mockMvc.perform(post("/courts/{courtId}/professional-information", courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(response.getId().toString()))
            .andExpect(jsonPath("$.courtId").value(courtId.toString()))
            .andExpect(jsonPath("$.interviewRoomCount").value(4));

        verify(professionalInformationService)
            .setProfessionalInformation(eq(courtId), any(CourtProfessionalInformation.class));
    }

    @Test
    void shouldReturnBadRequestWhenInterviewRoomCountInvalid() throws Exception {
        UUID courtId = UUID.randomUUID();
        CourtProfessionalInformation request = new CourtProfessionalInformation();
        request.setCourtId(courtId);
        request.setInterviewRooms(Boolean.TRUE);
        request.setInterviewRoomCount(0);
        request.setInterviewPhoneNumber("01234 567890");
        request.setVideoHearings(Boolean.TRUE);
        request.setCommonPlatform(Boolean.TRUE);
        request.setAccessScheme(Boolean.TRUE);

        mockMvc.perform(post("/courts/{courtId}/professional-information", courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.interviewRoomCount").value("must be greater than or equal to 1"));
    }
}
