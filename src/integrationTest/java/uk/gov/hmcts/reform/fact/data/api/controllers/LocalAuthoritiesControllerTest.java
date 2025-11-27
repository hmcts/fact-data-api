package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.services.LocalAuthoritiesService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalAuthoritiesController.class)
class LocalAuthoritiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocalAuthoritiesService localAuthoritiesService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID areaOfLawId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID localAuthorityId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns mappings successfully")
    void getLocalAuthoritiesReturnsOk() throws Exception {
        CourtLocalAuthorityDto response = CourtLocalAuthorityDto.builder()
            .areaOfLawId(areaOfLawId)
            .areaOfLawName("Adoption")
            .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                .id(localAuthorityId)
                .name("Authority One")
                .selected(true)
                .build()))
            .build();

        when(localAuthoritiesService.getCourtLocalAuthorities(courtId)).thenReturn(List.of(response));

        mockMvc.perform(get("/courts/{courtId}/v1/local-authorities", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].areaOfLawId").value(areaOfLawId.toString()))
            .andExpect(jsonPath("$[0].localAuthorities[0].name").value("Authority One"))
            .andExpect(jsonPath("$[0].localAuthorities[0].selected").value(true));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns 404 when court not found")
    void getLocalAuthoritiesReturnsNotFound() throws Exception {
        when(localAuthoritiesService.getCourtLocalAuthorities(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/local-authorities", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns 204 when areas not configured")
    void getLocalAuthoritiesReturnsNoContentWhenAreasMissing() throws Exception {
        when(localAuthoritiesService.getCourtLocalAuthorities(courtId))
            .thenThrow(new CourtResourceNotFoundException("No areas of law"));

        mockMvc.perform(get("/courts/{courtId}/v1/local-authorities", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns 400 for invalid UUID")
    void getLocalAuthoritiesInvalidUuid() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/local-authorities", "not-a-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities updates mappings successfully")
    void putLocalAuthoritiesReturnsOk() throws Exception {
        List<CourtLocalAuthorityDto> request = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(areaOfLawId)
            .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                .id(localAuthorityId)
                .selected(true)
                .build()))
            .build());

        mockMvc.perform(put("/courts/{courtId}/v1/local-authorities", courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().string("Update successful for court ID " + courtId));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 404 when court not found")
    void putLocalAuthoritiesReturnsNotFound() throws Exception {
        List<CourtLocalAuthorityDto> request = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(areaOfLawId)
            .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                .id(localAuthorityId)
                .selected(true)
                .build()))
            .build());

        doThrow(new NotFoundException("Court not found")).when(localAuthoritiesService)
            .setCourtLocalAuthorities(nonExistentCourtId, request);

        mockMvc.perform(put("/courts/{courtId}/v1/local-authorities", nonExistentCourtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 204 when areas not configured")
    void putLocalAuthoritiesReturnsNoContentWhenAreasMissing() throws Exception {
        List<CourtLocalAuthorityDto> request = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(areaOfLawId)
            .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                .id(localAuthorityId)
                .selected(true)
                .build()))
            .build());

        doThrow(new CourtResourceNotFoundException("No areas of law")).when(localAuthoritiesService)
            .setCourtLocalAuthorities(courtId, request);

        mockMvc.perform(put("/courts/{courtId}/v1/local-authorities", courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 400 for invalid UUID")
    void putLocalAuthoritiesInvalidUuid() throws Exception {
        List<CourtLocalAuthorityDto> request = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(areaOfLawId)
            .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                .id(localAuthorityId)
                .selected(true)
                .build()))
            .build());

        mockMvc.perform(put("/courts/{courtId}/v1/local-authorities", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 400 when request validation fails")
    void putLocalAuthoritiesValidationErrors() throws Exception {
        List<CourtLocalAuthorityDto> invalidRequest = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(null)
            .localAuthorities(null)
            .build());

        mockMvc.perform(put("/courts/{courtId}/v1/local-authorities", courtId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
