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
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAreasOfLawService;

import java.util.UUID;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@Feature("Court Areas Of Law Controller")
@DisplayName("Court Areas Of Law Controller")
@WebMvcTest(CourtAreasOfLawController.class)
class CourtAreasOfLawControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtAreasOfLawService courtAreasOfLawService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("GET /courts/{courtId}/v1/areas-of-law returns areas of law with status successfully")
    void shouldGetAreasOfLawForCourt() throws Exception {
        UUID civilId = UUID.randomUUID();
        UUID crimeId = UUID.randomUUID();

        Map<AreaOfLawType, Boolean> areasOfLaw = Map.of(
            new AreaOfLawType() {
                {
                    setId(civilId);
                    setName("Civil");
                }
            }, true,
            new AreaOfLawType() {
                {
                    setId(crimeId);
                    setName("Crime");
                }
            }, false
        );

        when(courtAreasOfLawService.getAreasOfLawStatusByCourtId(courtId))
            .thenReturn(areasOfLaw);

        mockMvc.perform(get("/courts/{courtId}/v1/areas-of-law", courtId)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['" + civilId + "']").value(true))
            .andExpect(jsonPath("$['" + crimeId + "']").value(false));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/areas-of-law returns 404 when court not found")
    void shouldReturn404WhenCourtNotFoundForGet() throws Exception {
        when(courtAreasOfLawService.getAreasOfLawStatusByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/areas-of-law", nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/areas-of-law creates areas of law")
    void shouldCreateCourtAreasOfLaw() throws Exception {
        CourtAreasOfLaw courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setCourtId(courtId);

        when(courtAreasOfLawService.setCourtAreasOfLaw(eq(courtId), any(CourtAreasOfLaw.class)))
            .thenReturn(courtAreasOfLaw);

        mockMvc.perform(put("/courts/{courtId}/v1/areas-of-law", courtId)
                        .content(objectMapper.writeValueAsString(courtAreasOfLaw))
                        .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.courtId").value(courtId.toString()));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/areas-of-law returns 404 when court not found")
    void shouldReturn404WhenCourtNotFoundForPut() throws Exception {
        CourtAreasOfLaw courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setCourtId(nonExistentCourtId);

        when(courtAreasOfLawService.setCourtAreasOfLaw(eq(nonExistentCourtId), any(CourtAreasOfLaw.class)))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(put("/courts/{courtId}/v1/areas-of-law", nonExistentCourtId)
                            .content(objectMapper.writeValueAsString(courtAreasOfLaw))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

}
