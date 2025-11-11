package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.services.TypesService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Types Controller")
@DisplayName("Types Controller")
@WebMvcTest(TypesController.class)
class TypesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TypesService typesService;

    @Test
    @DisplayName("GET /types/v1/areas-of-law returns areas of law types successfully")
    void getAreasOfLawTypesReturnsSuccessfully() throws Exception {
        List<AreaOfLawType> areaOfLawType = List.of(
            AreaOfLawType.builder()
            .id(UUID.randomUUID())
            .name("Name")
            .nameCy("NameCy")
            .build());

        when(typesService.getAreaOfLawTypes()).thenReturn(areaOfLawType);

        mockMvc.perform(get("/types/v1/areas-of-law"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Name"))
            .andExpect(jsonPath("$[0].nameCy").value("NameCy"));
    }

    @Test
    @DisplayName("GET /types/v1/areas-of-law returns empty list if areas of law types do not exist")
    void getAreasOfLawTypesReturnsNoContent() throws Exception {
        when(typesService.getAreaOfLawTypes()).thenReturn(List.of());

        mockMvc.perform(get("/types/v1/areas-of-law"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /types/v1/court-types returns court types successfully")
    void getCourtTypesReturnsSuccessfully() throws Exception {
        List<CourtType> courtTypes = List.of(
            CourtType.builder()
                .id(UUID.randomUUID())
                .name("Name")
                .build());

        when(typesService.getCourtTypes()).thenReturn(courtTypes);

        mockMvc.perform(get("/types/v1/court-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Name"));
    }

    @Test
    @DisplayName("GET /types/v1/court-types returns empty list if court types do not exist")
    void getCourtTypesReturnsNoContent() throws Exception {
        when(typesService.getCourtTypes()).thenReturn(List.of());

        mockMvc.perform(get("/types/v1/court-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /types/v1/opening-hours-types returns opening hours types successfully")
    void getOpeningHoursTypesReturnsSuccessfully() throws Exception {
        List<OpeningHourType> openingHourTypes = List.of(
            OpeningHourType.builder()
                .id(UUID.randomUUID())
                .name("Name")
                .nameCy("NameCy")
                .build());

        when(typesService.getOpeningHoursTypes()).thenReturn(openingHourTypes);

        mockMvc.perform(get("/types/v1/opening-hours-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Name"))
            .andExpect(jsonPath("$[0].nameCy").value("NameCy"));
    }

    @Test
    @DisplayName("GET /type/v1/opening-hours-types returns empty list if opening hours types do not exist")
    void getOpeningHoursTypesReturnsNoContent() throws Exception {
        when(typesService.getOpeningHoursTypes()).thenReturn(List.of());

        mockMvc.perform(get("/types/v1/opening-hours-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /types/v1/contact-description-types returns contact description types successfully")
    void getContactDescriptionTypesReturnsSuccessfully() throws Exception {
        List<ContactDescriptionType> contactDescriptionTypes = List.of(
            ContactDescriptionType.builder()
                .id(UUID.randomUUID())
                .name("Name")
                .nameCy("NameCy")
                .build());

        when(typesService.getContactDescriptionTypes()).thenReturn(contactDescriptionTypes);

        mockMvc.perform(get("/types/v1/contact-description-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Name"))
            .andExpect(jsonPath("$[0].nameCy").value("NameCy"));
    }

    @Test
    @DisplayName("GET /types/v1/contact-descriptions returns empty list if contact description types do not exist")
    void getContactDescriptionTypesReturnsNoContent() throws Exception {
        when(typesService.getContactDescriptionTypes()).thenReturn(List.of());

        mockMvc.perform(get("/types/v1/contact-description-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /types/v1/regions returns regions successfully")
    void getRegionsReturnsSuccessfully() throws Exception {
        List<Region> regions = List.of(
            Region.builder()
                .id(UUID.randomUUID())
                .name("Name")
                .build());

        when(typesService.getRegions()).thenReturn(regions);

        mockMvc.perform(get("/types/v1/regions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Name"));
    }

    @Test
    @DisplayName("GET /type/v1/regions returns empty list if regions do not exist")
    void getRegionsReturnsNoContent() throws Exception {
        when(typesService.getRegions()).thenReturn(List.of());

        mockMvc.perform(get("/types/v1/regions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /types/v1/service-areas returns service areas successfully")
    void getServiceAreasReturnsSuccessfully() throws Exception {
        List<ServiceArea> serviceAreas = List.of(
            ServiceArea.builder()
                .id(UUID.randomUUID())
                .name("Name")
                .nameCy("NameCy")
                .build());

        when(typesService.getServiceAreas()).thenReturn(serviceAreas);

        mockMvc.perform(get("/types/v1/service-areas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Name"))
            .andExpect(jsonPath("$[0].nameCy").value("NameCy"));
    }

    @Test
    @DisplayName("GET /types/v1/service-areas returns empty list if service areas do not exist")
    void getServiceAreasReturnsNoContent() throws Exception {
        when(typesService.getServiceAreas()).thenReturn(List.of());

        mockMvc.perform(get("/types/v1/service-areas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
