package uk.gov.hmcts.reform.fact.data.api.services.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchExecuterTest {

    @Mock
    private CourtAddressRepository courtAddressRepository;

    @Mock
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @InjectMocks
    private SearchExecuter searchExecuter;

    @Test
    void executeSearchStrategyShouldReturnNearestByAreaOfLawForDefault() {
        ServiceArea area = serviceArea(ServiceAreaType.CIVIL);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(courtAddressRepository.findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 10))
            .thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.DEFAULT_AOL_DISTANCE,
            SearchAction.NEAREST,
            10
        );

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository).findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 10);
    }

    @Test
    void executeSearchStrategyShouldReturnCivilTieredResultsWhenAvailable() {
        ServiceArea area = serviceArea(ServiceAreaType.CIVIL);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(courtAddressRepository.findCivilByPartialPostcodeBestTier(
            eq(area.getId()),
            eq(51.5),
            eq(-0.1),
            eq("SW1A1"),
            eq("SW1A"),
            eq("SW"),
            eq(5)
        )).thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.CIVIL_POSTCODE_PREFERENCE,
            SearchAction.DOCUMENTS,
            5
        );

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository, never()).findNearestByAreaOfLaw(anyDouble(), anyDouble(), any(), anyInt());
    }

    @Test
    void executeSearchStrategyShouldFallbackToNearestWhenCivilTieredResultsEmpty() {
        ServiceArea area = serviceArea(ServiceAreaType.CIVIL);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(courtAddressRepository.findCivilByPartialPostcodeBestTier(
            eq(area.getId()),
            eq(51.5),
            eq(-0.1),
            eq("SW1A1"),
            eq("SW1A"),
            eq("SW"),
            eq(5)
        )).thenReturn(List.of());
        when(courtAddressRepository.findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 5))
            .thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.CIVIL_POSTCODE_PREFERENCE,
            SearchAction.UPDATE,
            5
        );

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository).findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 5);
    }

    @Test
    void executeSearchStrategyShouldReturnFamilyRegionalByLocalAuthorityWhenFound() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        LocalAuthorityType authorityType = localAuthorityType(UUID.randomUUID());
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Authority"))
            .thenReturn(Optional.of(authorityType));
        when(courtAddressRepository.findFamilyRegionalByLocalAuthority(
            area.getId(),
            51.5,
            -0.1,
            area.getAreaOfLawId(),
            authorityType.getId()
        )).thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository, never()).findFamilyRegionalByAol(any(), anyDouble(), anyDouble(), any());
    }

    @Test
    void executeSearchStrategyShouldFallbackToRegionalByAolWhenLocalAuthorityEmpty() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        LocalAuthorityType authorityType = localAuthorityType(UUID.randomUUID());
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Authority"))
            .thenReturn(Optional.of(authorityType));
        when(courtAddressRepository.findFamilyRegionalByLocalAuthority(
            area.getId(),
            51.5,
            -0.1,
            area.getAreaOfLawId(),
            authorityType.getId()
        )).thenReturn(List.of());
        when(courtAddressRepository.findFamilyRegionalByAol(area.getId(), 51.5, -0.1, area.getAreaOfLawId()))
            .thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository, never()).findNearestByAreaOfLaw(anyDouble(), anyDouble(), any(), anyInt());
    }

    @Test
    void executeSearchStrategyShouldFallbackToNearestWhenRegionalResultsMissing() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        LocalAuthorityType authorityType = localAuthorityType(UUID.randomUUID());
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Authority"))
            .thenReturn(Optional.of(authorityType));
        when(courtAddressRepository.findFamilyRegionalByLocalAuthority(
            area.getId(),
            51.5,
            -0.1,
            area.getAreaOfLawId(),
            authorityType.getId()
        )).thenReturn(List.of());
        when(courtAddressRepository.findFamilyRegionalByAol(area.getId(), 51.5, -0.1, area.getAreaOfLawId()))
            .thenReturn(List.of());
        when(courtAddressRepository.findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 10))
            .thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(results);
    }

    @Test
    void executeSearchStrategyShouldReturnFamilyNonRegionalByLocalAuthorityWhenFound() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        LocalAuthorityType authorityType = localAuthorityType(UUID.randomUUID());
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Authority"))
            .thenReturn(Optional.of(authorityType));
        when(courtAddressRepository.findFamilyNonRegionalByLocalAuthority(
            51.5,
            -0.1,
            area.getAreaOfLawId(),
            authorityType.getId(),
            10
        )).thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_NON_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository, never()).findNearestByAreaOfLaw(anyDouble(), anyDouble(), any(), anyInt());
    }

    @Test
    void executeSearchStrategyShouldFallbackToNearestWhenNonRegionalByLocalAuthorityMissing() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Authority"))
            .thenReturn(Optional.empty());
        when(courtAddressRepository.findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 10))
            .thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_NON_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(results);
    }

    @Test
    void executeSearchStrategyShouldUseStrippedCouncilNameWhenExactMatchMissing() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Test Council", "SW1A 1AA");
        LocalAuthorityType authorityType = localAuthorityType(UUID.randomUUID());
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Test Council"))
            .thenReturn(Optional.empty());
        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Test"))
            .thenReturn(Optional.of(authorityType));
        when(courtAddressRepository.findFamilyNonRegionalByLocalAuthority(
            51.5,
            -0.1,
            area.getAreaOfLawId(),
            authorityType.getId(),
            10
        )).thenReturn(results);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_NON_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(results);
        verify(localAuthorityTypeRepository).findIdByNameIgnoreCase("Test Council");
        verify(localAuthorityTypeRepository).findIdByNameIgnoreCase("Test");
    }

    private ServiceArea serviceArea(ServiceAreaType type) {
        ServiceArea area = new ServiceArea();
        area.setId(UUID.randomUUID());
        area.setAreaOfLawId(UUID.randomUUID());
        area.setType(type);
        area.setAreaOfLaw(AreaOfLawType.builder().name("Civil").build());
        return area;
    }

    private OsLocationData osLocationData(String authorityName, String postcode) {
        return OsLocationData.builder()
            .authorityName(authorityName)
            .postcode(postcode)
            .latitude(51.5)
            .longitude(-0.1)
            .build();
    }

    private LocalAuthorityType localAuthorityType(UUID id) {
        LocalAuthorityType authorityType = new LocalAuthorityType();
        authorityType.setId(id);
        authorityType.setName("Authority");
        return authorityType;
    }
}
