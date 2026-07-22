package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.services.AllLocationService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("All Location Controller")
@WebMvcTest(AllLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AllLocationControllerTest {

    private static final UUID LOCATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AllLocationService allLocationService;

    @Test
    @DisplayName("GET /all/v1 returns combined paginated locations")
    void getAllLocationsReturnsCombinedPaginatedLocations() throws Exception {
        when(allLocationService.getFilteredAndPaginatedLocations(
            anyInt(),
            anyInt(),
            eq(true),
            eq(false),
            nullable(String.class),
            eq("Test"),
            nullable(String.class),
            nullable(String.class)
        )).thenReturn(page(buildLocation("COURT", false)));

        mockMvc.perform(get("/all/v1")
                            .param("pageNumber", "0")
                            .param("pageSize", "25")
                            .param("includeClosed", "true")
                            .param("onlyServiceCentres", "false")
                            .param("partialCourtName", "Test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(LOCATION_ID.toString()))
            .andExpect(jsonPath("$.content[0].locationType").value("COURT"))
            .andExpect(jsonPath("$.content[0].favourite").doesNotExist());
    }

    @Test
    @DisplayName("GET /all/v1 can return service centres only")
    void getAllLocationsCanReturnServiceCentresOnly() throws Exception {
        when(allLocationService.getFilteredAndPaginatedLocations(
            anyInt(),
            anyInt(),
            eq(false),
            eq(true),
            nullable(String.class),
            nullable(String.class),
            nullable(String.class),
            nullable(String.class)
        )).thenReturn(page(buildLocation("SERVICE_CENTRE", true)));

        mockMvc.perform(get("/all/v1")
                            .param("onlyServiceCentres", "true")
                            .param("includeClosed", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].locationType").value("SERVICE_CENTRE"))
            .andExpect(jsonPath("$.content[0].serviceCentre").value(true));
    }

    @Test
    @DisplayName("GET /all/details/v1 returns combined location details")
    void getAllLocationDetailsReturnsCombinedLocationDetails() throws Exception {
        when(allLocationService.getAllLocationDetails()).thenReturn(List.of(
            AllLocationDetails.fromCourt(buildCourtDetails()),
            AllLocationDetails.fromServiceCentre(buildServiceCentreDetails())
        ));

        mockMvc.perform(get("/all/details/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationType").value("COURT"))
            .andExpect(jsonPath("$[0].court.name").value("Test Court"))
            .andExpect(jsonPath("$[1].locationType").value("SERVICE_CENTRE"))
            .andExpect(jsonPath("$[1].serviceCentreDetails.name").value("Test Service Centre"));
    }

    @Test
    @DisplayName("GET /all/details.json returns combined location details")
    void getAllLocationDetailsJsonReturnsCombinedLocationDetails() throws Exception {
        when(allLocationService.getAllLocationDetails()).thenReturn(List.of(
            AllLocationDetails.fromServiceCentre(buildServiceCentreDetails())
        ));

        mockMvc.perform(get("/all/details.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationType").value("SERVICE_CENTRE"));
    }

    private Page<AllLocation> page(AllLocation location) {
        return new PageImpl<>(List.of(location));
    }

    private AllLocation buildLocation(String locationType, boolean serviceCentre) {
        return AllLocation.builder()
            .id(LOCATION_ID)
            .name("Test Location")
            .slug("test-location")
            .open(true)
            .locationType(locationType)
            .serviceCentre(serviceCentre)
            .build();
    }

    private CourtDetails buildCourtDetails() {
        return CourtDetails.builder()
            .id(LOCATION_ID)
            .name("Test Court")
            .slug("test-court")
            .open(true)
            .build();
    }

    private ServiceCentreDetails buildServiceCentreDetails() {
        return ServiceCentreDetails.builder()
            .id(LOCATION_ID)
            .name("Test Service Centre")
            .slug("test-service-centre")
            .open(true)
            .build();
    }
}
