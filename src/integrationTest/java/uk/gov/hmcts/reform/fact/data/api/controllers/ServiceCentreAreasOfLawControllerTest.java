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
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAreasOfLawService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Service Centre Areas Of Law Controller")
@DisplayName("Service Centre Areas Of Law Controller")
@WebMvcTest(ServiceCentreAreasOfLawController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ServiceCentreAreasOfLawControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID AREA_OF_LAW_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceCentreAreasOfLawService serviceCentreAreasOfLawService;

    @Test
    void getAreasOfLawReturnsMap() throws Exception {
        AreaOfLawType areaOfLawType = AreaOfLawType.builder()
            .id(AREA_OF_LAW_ID)
            .name("Civil")
            .displayName("Civil")
            .build();

        when(serviceCentreAreasOfLawService.getAreasOfLawStatusByServiceCentreId(SERVICE_CENTRE_ID))
            .thenReturn(Map.of(areaOfLawType, true));

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/areas-of-law", SERVICE_CENTRE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.*").exists());
    }

    @Test
    void getAreasOfLawReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/areas-of-law", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAreasOfLawReturnsNotFoundWhenMissing() throws Exception {
        when(serviceCentreAreasOfLawService.getAreasOfLawStatusByServiceCentreId(SERVICE_CENTRE_ID))
            .thenThrow(new NotFoundException("Missing"));

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/areas-of-law", SERVICE_CENTRE_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void putAreasOfLawReturnsCreated() throws Exception {
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(SERVICE_CENTRE_ID)
            .areasOfLaw(List.of(AREA_OF_LAW_ID))
            .build();

        when(serviceCentreAreasOfLawService.setServiceCentreAreasOfLaw(
            eq(SERVICE_CENTRE_ID),
            any(ServiceCentreAreasOfLaw.class)
        )).thenReturn(areasOfLaw);

        mockMvc.perform(put("/service-centres/{serviceCentreId}/v1/areas-of-law", SERVICE_CENTRE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(areasOfLaw)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.serviceCentreId").value(SERVICE_CENTRE_ID.toString()));
    }
}
