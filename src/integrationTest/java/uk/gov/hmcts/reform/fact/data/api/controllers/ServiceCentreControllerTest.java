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
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreDetailsViewService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Service Centre Controller")
@DisplayName("Service Centre Controller")
@WebMvcTest(ServiceCentreController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ServiceCentreControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID CONTACT_DESCRIPTION_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
    private static final UUID AREA_OF_LAW_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
    private static final UUID SERVICE_AREA_ID = UUID.fromString("423e4567-e89b-12d3-a456-426614174000");
    private static final String SERVICE_CENTRE_NAME = "Test Service Centre";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceCentreService serviceCentreService;

    @MockitoBean
    private ServiceCentreDetailsViewService serviceCentreDetailsViewService;

    @Test
    @DisplayName("GET /service-centres/{serviceCentreId}/v1 returns service centre details")
    void getServiceCentreDetailsByIdReturnsServiceCentreDetails() throws Exception {
        ServiceCentreDetails serviceCentreDetails = buildServiceCentreDetails();

        when(serviceCentreService.getServiceCentreDetailsById(SERVICE_CENTRE_ID)).thenReturn(serviceCentreDetails);
        when(serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails)).thenReturn(serviceCentreDetails);

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1", SERVICE_CENTRE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(SERVICE_CENTRE_ID.toString()))
            .andExpect(jsonPath("$.name").value(SERVICE_CENTRE_NAME))
            .andExpect(jsonPath("$.serviceAreaIds").doesNotExist())
            .andExpect(jsonPath("$.serviceAreas[0].id").value(SERVICE_AREA_ID.toString()))
            .andExpect(jsonPath("$.serviceAreas[0].name").value("Family"))
            .andExpect(jsonPath("$.serviceCentreContactDetails[0].serviceCentreContactDescriptionId").doesNotExist())
            .andExpect(jsonPath("$.serviceCentreContactDetails[0].serviceCentreContactDescription.id")
                           .value(CONTACT_DESCRIPTION_ID.toString()))
            .andExpect(jsonPath("$.serviceCentreContactDetails[0].serviceCentreContactDescription.name")
                           .value("Enquiries"))
            .andExpect(jsonPath("$.serviceCentreAreasOfLaw[0].areasOfLaw[0].id").value(AREA_OF_LAW_ID.toString()))
            .andExpect(jsonPath("$.serviceCentreAreasOfLaw[0].areasOfLaw[0].name").value("Civil"));
    }

    @Test
    @DisplayName("GET /service-centres/slug/{serviceCentreSlug}/v1 returns service centre details")
    void getServiceCentreDetailsBySlugReturnsServiceCentreDetails() throws Exception {
        ServiceCentreDetails serviceCentreDetails = buildServiceCentreDetails();

        when(serviceCentreService.getServiceCentreDetailsBySlug("test-service-centre"))
            .thenReturn(serviceCentreDetails);
        when(serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails)).thenReturn(serviceCentreDetails);

        mockMvc.perform(get("/service-centres/slug/{serviceCentreSlug}/v1", "test-service-centre"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(SERVICE_CENTRE_ID.toString()))
            .andExpect(jsonPath("$.slug").value("test-service-centre"))
            .andExpect(jsonPath("$.serviceAreas[0].id").value(SERVICE_AREA_ID.toString()));
    }

    @Test
    @DisplayName("GET /service-centres/{serviceCentreId}/v1 returns 400 for invalid UUID")
    void getServiceCentreByIdReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /service-centres/{serviceCentreId}/v1 returns 404 when missing")
    void getServiceCentreByIdReturnsNotFound() throws Exception {
        when(serviceCentreService.getServiceCentreDetailsById(SERVICE_CENTRE_ID))
            .thenThrow(new NotFoundException("Service centre not found"));

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1", SERVICE_CENTRE_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /service-centres/{serviceCentreId}/entity/v1 returns service centre entity")
    void getServiceCentreEntityByIdReturnsServiceCentre() throws Exception {
        ServiceCentre serviceCentre = buildServiceCentre();

        when(serviceCentreService.getServiceCentreById(SERVICE_CENTRE_ID)).thenReturn(serviceCentre);

        mockMvc.perform(get("/service-centres/{serviceCentreId}/entity/v1", SERVICE_CENTRE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(SERVICE_CENTRE_ID.toString()))
            .andExpect(jsonPath("$.name").value(SERVICE_CENTRE_NAME));
    }

    @Test
    @DisplayName("GET /service-centres/name/v1 returns service centre for exact name")
    void getServiceCentreByNameReturnsServiceCentre() throws Exception {
        ServiceCentre serviceCentre = buildServiceCentre();

        when(serviceCentreService.getServiceCentreByName(SERVICE_CENTRE_NAME)).thenReturn(serviceCentre);

        mockMvc.perform(get("/service-centres/name/v1").param("name", SERVICE_CENTRE_NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(SERVICE_CENTRE_ID.toString()))
            .andExpect(jsonPath("$.name").value(SERVICE_CENTRE_NAME));
    }

    @Test
    @DisplayName("POST /service-centres/v1 creates service centre")
    void createServiceCentreReturnsCreated() throws Exception {
        ServiceCentre serviceCentre = buildServiceCentre();

        when(serviceCentreService.createServiceCentre(any(ServiceCentre.class))).thenReturn(serviceCentre);

        mockMvc.perform(post("/service-centres/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(serviceCentre)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(SERVICE_CENTRE_ID.toString()));
    }

    @Test
    @DisplayName("PUT /service-centres/{serviceCentreId}/v1 updates service centre")
    void updateServiceCentreReturnsUpdated() throws Exception {
        ServiceCentre serviceCentre = buildServiceCentre();

        when(serviceCentreService.updateServiceCentre(eq(SERVICE_CENTRE_ID), any(ServiceCentre.class)))
            .thenReturn(serviceCentre);

        mockMvc.perform(put("/service-centres/{serviceCentreId}/v1", SERVICE_CENTRE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(serviceCentre)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(SERVICE_CENTRE_ID.toString()));
    }

    private ServiceCentre buildServiceCentre() {
        return ServiceCentre.builder()
            .id(SERVICE_CENTRE_ID)
            .name(SERVICE_CENTRE_NAME)
            .slug("test-service-centre")
            .open(true)
            .catchmentType(CatchmentType.REGIONAL)
            .build();
    }

    private ServiceCentreDetails buildServiceCentreDetails() {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(CONTACT_DESCRIPTION_ID)
            .serviceCentreContactDescriptionDetails(ContactDescriptionType.builder()
                                                        .id(CONTACT_DESCRIPTION_ID)
                                                        .name("Enquiries")
                                                        .build())
            .build();
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(AREA_OF_LAW_ID))
            .areasOfLawDetails(List.of(AreaOfLawType.builder()
                                          .id(AREA_OF_LAW_ID)
                                          .name("Civil")
                                          .build()))
            .build();

        return ServiceCentreDetails.builder()
            .id(SERVICE_CENTRE_ID)
            .name(SERVICE_CENTRE_NAME)
            .slug("test-service-centre")
            .open(true)
            .catchmentType(CatchmentType.NATIONAL)
            .serviceAreaIds(List.of(SERVICE_AREA_ID))
            .serviceAreaDetails(List.of(ServiceArea.builder()
                                            .id(SERVICE_AREA_ID)
                                            .name("Family")
                                            .build()))
            .serviceCentreContactDetails(List.of(contactDetails))
            .serviceCentreAreasOfLaw(List.of(areasOfLaw))
            .build();
    }
}
