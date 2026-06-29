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
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @Mock
    private CourtDetailsViewService courtDetailsViewService;

    @Mock
    private ServiceCentreDetailsViewService serviceCentreDetailsViewService;

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
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(AllLocation::getLocationType)
            .containsExactly("COURT", "SERVICE_CENTRE");
    }

    @Test
    void getFilteredAndPaginatedLocationsPreservesRepositoryOrderWhenSortIsNotRequested() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Charlie Court", true),
            buildCourt("Alpha Court", true)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            null,
            null
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("Charlie Court", "Alpha Court", "Beta Service Centre");
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
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::getName)
            .isEqualTo("Open Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsFiltersClosedCourtsUnlessIncluded() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Open Court", true),
            buildCourt("Closed Court", false)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of());

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::getName)
            .isEqualTo("Open Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsIncludesClosedLocationsWhenRequested() {
        when(courtRepository.findAll()).thenReturn(List.of(buildCourt("Open Court", true)));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Closed Service Centre", false)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            true,
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("Closed Service Centre", "Open Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsReturnsServiceCentresOnlyWhenRequested() {
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            true,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::getLocationType)
            .isEqualTo("SERVICE_CENTRE");
    }

    @Test
    void getFilteredAndPaginatedLocationsFiltersByPartialNameCaseInsensitively() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Alpha Court", true),
            buildCourt("Beta Court", true)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(
            buildServiceCentre("Alpha Service Centre", true)
        ));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            "aLpHa",
            "name",
            "asc"
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("Alpha Court", "Alpha Service Centre");
    }

    @Test
    void getOpenLocationsByPrefixReturnsCourtsAndServiceCentresOrderedByName() {
        when(courtRepository.findCourtByNameStartingWithIgnoreCaseAndOpenOrderByNameAsc("A", true))
            .thenReturn(List.of(
                buildCourt("Alpha Court", true),
                buildCourt("Arlington Court", true)
            ));
        when(serviceCentreRepository.findByNameStartingWithIgnoreCaseAndOpenOrderByNameAsc("A", true))
            .thenReturn(List.of(
                buildServiceCentre("Aardvark Service Centre", true),
                buildServiceCentre("Alpha Service Centre", true)
            ));

        List<AllLocation> result = allLocationService.getOpenLocationsByPrefix("A");

        assertThat(result)
            .extracting(AllLocation::getName)
            .containsExactly(
                "Aardvark Service Centre",
                "Alpha Court",
                "Alpha Service Centre",
                "Arlington Court"
            );
        assertThat(result)
            .extracting(AllLocation::getLocationType)
            .containsExactly("SERVICE_CENTRE", "COURT", "SERVICE_CENTRE", "COURT");
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
            false,
            REGION_ID.toString(),
            null,
            "name",
            "asc"
        );

        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::getName)
            .isEqualTo("Matching Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsDefaultsSortOrderToAscendingWhenSortByIsProvided() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Charlie Court", true),
            buildCourt("Alpha Court", true)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "name",
            null
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("Alpha Court", "Beta Service Centre", "Charlie Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsSortsByNameDescending() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Alpha Court", true),
            buildCourt("Charlie Court", true)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "name",
            "desc"
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("Charlie Court", "Beta Service Centre", "Alpha Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsSortsByLastUpdatedAscendingAndThenName() {
        ZonedDateTime sameLastUpdatedAt = ZonedDateTime.parse("2026-01-01T10:00:00Z");
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Beta Court", true, sameLastUpdatedAt),
            buildCourt("Alpha Court", true, sameLastUpdatedAt)
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(
            buildServiceCentre("Later Service Centre", true, ZonedDateTime.parse("2026-01-02T10:00:00Z"))
        ));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "lastUpdated",
            "asc"
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("Alpha Court", "Beta Court", "Later Service Centre");
    }

    @Test
    void getFilteredAndPaginatedLocationsSortsByLastUpdatedDescending() {
        when(courtRepository.findAll()).thenReturn(List.of(
            buildCourt("Old Court", true, ZonedDateTime.parse("2026-01-01T10:00:00Z")),
            buildCourt("New Court", true, ZonedDateTime.parse("2026-01-03T10:00:00Z"))
        ));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(
            buildServiceCentre("Middle Service Centre", true, ZonedDateTime.parse("2026-01-02T10:00:00Z"))
        ));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "lastUpdated",
            "desc"
        );

        assertThat(result.getContent())
            .extracting(AllLocation::getName)
            .containsExactly("New Court", "Middle Service Centre", "Old Court");
    }

    @Test
    void getFilteredAndPaginatedLocationsReturnsEmptyPageWhenPageNumberIsOutOfRange() {
        when(courtRepository.findAll()).thenReturn(List.of(buildCourt("Alpha Court", true)));
        when(serviceCentreRepository.findAll()).thenReturn(List.of(buildServiceCentre("Beta Service Centre", true)));

        Page<AllLocation> result = allLocationService.getFilteredAndPaginatedLocations(
            10,
            25,
            false,
            false,
            null,
            null,
            null,
            null
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getFilteredAndPaginatedLocationsRejectsSortOrderWithoutSortBy() {
        when(courtRepository.findAll()).thenReturn(List.of(buildCourt("Alpha Court", true)));
        when(serviceCentreRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            null,
            "asc"
        ))
            .isInstanceOf(InvalidParameterCombinationException.class)
            .hasMessage("sortOrder cannot be provided without sortBy");
    }

    @Test
    void getFilteredAndPaginatedLocationsRejectsInvalidSortBy() {
        when(courtRepository.findAll()).thenReturn(List.of(buildCourt("Alpha Court", true)));
        when(serviceCentreRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "createdAt",
            "asc"
        ))
            .isInstanceOf(InvalidParameterCombinationException.class)
            .hasMessage("sortBy must be one of: name, lastUpdated");
    }

    @Test
    void getFilteredAndPaginatedLocationsRejectsInvalidSortOrder() {
        when(courtRepository.findAll()).thenReturn(List.of(buildCourt("Alpha Court", true)));
        when(serviceCentreRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> allLocationService.getFilteredAndPaginatedLocations(
            0,
            25,
            false,
            false,
            null,
            null,
            "name",
            "sideways"
        ))
            .isInstanceOf(InvalidParameterCombinationException.class)
            .hasMessage("sortOrder must be one of: asc, desc");
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
            false,
            null,
            null,
            "name",
            "asc"
        );

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).singleElement()
            .extracting(AllLocation::getName)
            .isEqualTo("Beta Service Centre");
    }

    @Test
    void getAllLocationDetailsReturnsCourtAndServiceCentreDetails() {
        CourtDetails courtDetails = buildCourtDetails("Alpha Court");
        ServiceCentreDetails serviceCentreDetails = buildServiceCentreDetails("Beta Service Centre");

        when(courtDetailsRepository.findAll()).thenReturn(List.of(courtDetails));
        when(serviceCentreDetailsRepository.findAll()).thenReturn(List.of(
            serviceCentreDetails
        ));
        when(courtDetailsViewService.prepareDetailsView(any(CourtDetails.class))).thenAnswer(
            invocation -> invocation.getArgument(0)
        );
        when(serviceCentreDetailsViewService.prepareDetailsView(any(ServiceCentreDetails.class))).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        List<AllLocationDetails> result = allLocationService.getAllLocationDetails();

        assertThat(result)
            .extracting(AllLocationDetails::getLocationType)
            .containsExactly("COURT", "SERVICE_CENTRE");
        verify(courtDetailsViewService).prepareDetailsView(courtDetails);
        verify(serviceCentreDetailsViewService).prepareDetailsView(serviceCentreDetails);
    }

    private Court buildCourt(String name, boolean open) {
        return buildCourt(name, open, null);
    }

    private Court buildCourt(String name, boolean open, ZonedDateTime lastUpdatedAt) {
        return Court.builder()
            .id(UUID.randomUUID())
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .open(open)
            .regionId(REGION_ID)
            .lastUpdatedAt(lastUpdatedAt)
            .build();
    }

    private ServiceCentre buildServiceCentre(String name, boolean open) {
        return buildServiceCentre(name, open, null);
    }

    private ServiceCentre buildServiceCentre(String name, boolean open, ZonedDateTime lastUpdatedAt) {
        return ServiceCentre.builder()
            .id(UUID.randomUUID())
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .open(open)
            .lastUpdatedAt(lastUpdatedAt)
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
