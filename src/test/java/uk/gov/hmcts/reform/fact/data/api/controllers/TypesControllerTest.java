package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.services.TypesService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypesControllerTest {

    private static final UUID TYPE_ID = UUID.randomUUID();

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private TypesService typesService;

    @InjectMocks
    private TypesController typesController;

    @Test
    void getAreasOfLawTypesReturns200() {
        List<AreaOfLawType> areasOfLawTypes = List.of(
            AreaOfLawType.builder().id(TYPE_ID).name("Name").nameCy("NameCy").build());

        when(typesService.getAreaOfLawTypes()).thenReturn(areasOfLawTypes);

        var response = typesController.getAreasOfLawTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(areasOfLawTypes);
    }

    @Test
    void getAreasOfLawTypesReturnsEmptyList() {
        List<AreaOfLawType> emptyList = List.of();
        when(typesService.getAreaOfLawTypes()).thenReturn(emptyList);

        var response = typesController.getAreasOfLawTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }


    @Test
    void getCourtTypesReturns200() {
        List<CourtType> courtTypes = List.of(
            CourtType.builder().id(TYPE_ID).name("Name").build());

        when(typesService.getCourtTypes()).thenReturn(courtTypes);

        var response = typesController.getCourtTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courtTypes);
    }

    @Test
    void getCourtTypesReturnsEmptyList() {
        List<CourtType> emptyList = List.of();
        when(typesService.getCourtTypes()).thenReturn(emptyList);

        var response = typesController.getCourtTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }

    @Test
    void getOpeningHoursTypesReturns200() {
        List<OpeningHourType> openingHourTypes = List.of(
            OpeningHourType.builder().id(TYPE_ID).name("Name").nameCy("NameCy").build());

        when(typesService.getOpeningHoursTypes()).thenReturn(openingHourTypes);

        var response = typesController.getOpeningHoursTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(openingHourTypes);
    }

    @Test
    void getOpeningHoursTypesReturnsEmptyList() {
        List<OpeningHourType> emptyList = List.of();
        when(typesService.getOpeningHoursTypes()).thenReturn(emptyList);

        var response = typesController.getOpeningHoursTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }

    @Test
    void getContactDescriptionTypesReturns200() {
        List<ContactDescriptionType> contactDescriptionTypes = List.of(
            ContactDescriptionType.builder().id(TYPE_ID).name("Name").nameCy("NameCy").build());

        when(typesService.getContactDescriptionTypes()).thenReturn(contactDescriptionTypes);

        var response = typesController.getContactDescriptionTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(contactDescriptionTypes);
    }

    @Test
    void getContactDescriptionTypesReturnsEmptyList() {
        List<ContactDescriptionType> emptyList = List.of();
        when(typesService.getContactDescriptionTypes()).thenReturn(emptyList);

        var response = typesController.getContactDescriptionTypes();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }

    @Test
    void getRegionsReturns200() {
        List<Region> regions = List.of(
            Region.builder().id(TYPE_ID).name("Name").country("GBR").build());

        when(typesService.getRegions()).thenReturn(regions);

        var response = typesController.getRegions();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(regions);
    }

    @Test
    void getRegionsReturnsEmptyList() {
        List<Region> emptyList = List.of();
        when(typesService.getRegions()).thenReturn(emptyList);

        var response = typesController.getRegions();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }

    @Test
    void getServiceAreasReturns200() {
        List<ServiceArea> serviceAreas = List.of(
            ServiceArea.builder().id(TYPE_ID).name("Name").nameCy("NameCy").build());

        when(typesService.getServiceAreas()).thenReturn(serviceAreas);

        var response = typesController.getServiceAreas();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(serviceAreas);
    }

    @Test
    void getServiceAreasReturnsEmptyList() {
        List<ServiceArea> emptyList = List.of();
        when(typesService.getServiceAreas()).thenReturn(emptyList);

        var response = typesController.getServiceAreas();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }
}
