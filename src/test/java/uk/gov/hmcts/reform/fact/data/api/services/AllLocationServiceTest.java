package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllLocationServiceTest {

    private static final UUID REGION_ID = UUID.randomUUID();

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ServiceCentreRepository serviceCentreRepository;

    @Mock
    private CourtDetailsRepository courtDetailsRepository;

    @Mock
    private ServiceCentreDetailsRepository serviceCentreDetailsRepository;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private AllLocationService allLocationService;

    @Test
    void getFilteredAndPaginatedLocationsReturnsCourtsAndServiceCentres() {
        Court court = buildCourt("Alpha Court", true);
        ServiceCentre serviceCentre = buildServiceCentre("Beta Service Centre", true);

        when(courtRepository.findAll()).thenReturn(List.of(court));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(serviceCentre));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(AllLocation::locationType)
            .containsExactly("COURT", "SERVICE_CENTRE");
    }

    @Test
    void getFilteredAndPaginatedLocationsFiltersClosedLocationsUnlessIncluded() {
        Court openCourt = buildCourt("Open Court", true);
        ServiceCentre closedServiceCentre = buildServiceCentre("Closed Service Centre", false);

        when(courtRepository.findAll()).thenReturn(List.of(openCourt));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(closedServiceCentre));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::name)
            .isEqualTo("Open Court");
    }

    @Test
    void getFilteredAndPaginatedCourtsExcludesServiceCentres() {
        when(courtRepository.findAll()).thenReturn(List.of(buildCourt("Alpha Court", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedCourts(
            0,
            25,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::locationType)
            .isEqualTo("COURT");
    }

    @Test
    void getFilteredAndPaginatedServiceCentresExcludesCourts() {
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedServiceCentres(
            0,
            25,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::locationType)
            .isEqualTo("SERVICE_CENTRE");
    }

    @Test
    void getFilteredAndPaginatedLocationsAppliesRegionFilterToCourtsAndExcludesServiceCentres() {
        Court matchingCourt = buildCourt("Matching Court", true);
        Court otherCourt = buildCourt("Other Court", true);
        otherCourt.setRegionId(UUID.randomUUID());

        when(regionService.getRegionById(REGION_ID)).thenReturn(Region.builder().id(REGION_ID).build());
        when(courtRepository.findAll()).thenReturn(List.of(matchingCourt, otherCourt));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            REGION_ID.toString(),
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::name)
            .isEqualTo("Matching Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsPaginatesCombinedResults() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Alpha Court", true),
            buildCourt("Charlie Court", true)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            1,
            1,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::name)
            .isEqualTo("Beta Service Centre");
    }

    @Test
    void getAllLocationDetailsReturnsCourtAndServiceCentreDetails() {
        when(courtDetailsRepository.findAll()).thenReturn(List.of(buildCourtDetails("Alpha Court")));
        when(serviceCentreDetailsRepository.findAll()).thenReturn(List.of(
            buildServiceCentreDetails("Beta Service Centre")
        ));

        List<AllLocationDetails> result = allLocationService.getAllLocationDetails();

        assertThat(result)
            .extracting(AllLocationDetails::locationType)
            .containsExactly("COURT", "SERVICE_CENTRE");
    }

    @Test
    void getAllCourtDetailsReturnsCourtDetailsOnly() {
        when(courtDetailsRepository.findAll()).thenReturn(List.of(buildCourtDetails("Alpha Court")));

        List<AllLocationDetails> result = allLocationService.getAllCourtDetails();

        assertThat(result).singleElement()
            .extracting(AllLocationDetails::locationType)
            .isEqualTo("COURT");
    }

    @Test
    void getAllServiceCentreDetailsReturnsServiceCentreDetailsOnly() {
        when(serviceCentreDetailsRepository.findAll()).thenReturn(List.of(
            buildServiceCentreDetails("Beta Service Centre")
        ));

        List<AllLocationDetails> result = allLocationService.getAllServiceCentreDetails();

        assertThat(result).singleElement()
            .extracting(AllLocationDetails::locationType)
            .isEqualTo("SERVICE_CENTRE");
    }

    private Court buildCourt(String name, boolean open) {
        return Court.builder()
            .id(UUID.randomUUID())
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .open(open)
            .regionId(REGION_ID)
            .build();
    }

    private ServiceCentre buildServiceCentre(String name, boolean open) {
        return ServiceCentre.builder()
            .id(UUID.randomUUID())
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .open(open)
            .build();
    }

    private CourtDetails buildCourtDetails(String name) {
        return CourtDetails.builder()
            .id(UUID.randomUUID())
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .open(true)
            .regionId(REGION_ID)
            .build();
    }

    private ServiceCentreDetails buildServiceCentreDetails(String name) {
        return ServiceCentreDetails.builder()
            .id(UUID.randomUUID())
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .open(true)
            .build();
    }
}
