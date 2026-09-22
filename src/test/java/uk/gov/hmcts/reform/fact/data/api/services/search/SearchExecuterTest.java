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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
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
    void executeSearchStrategyShouldReturnEachCourtOnceUsingNearestResult() {
        ServiceArea area = serviceArea(ServiceAreaType.CIVIL);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        UUID firstCourtId = UUID.randomUUID();
        UUID secondCourtId = UUID.randomUUID();
        UUID thirdCourtId = UUID.randomUUID();
        CourtWithDistance distantDuplicate = courtWithDistance(firstCourtId, "10");
        CourtWithDistance secondCourt = courtWithDistance(secondCourtId, "2");
        CourtWithDistance thirdCourt = courtWithDistance(thirdCourtId, "3");
        CourtWithDistance nearestDuplicate = courtWithDistance(firstCourtId, "1");
        CourtWithDistance laterDistantDuplicate = courtWithDistance(firstCourtId, "20");

        when(courtAddressRepository.findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 2))
            .thenReturn(List.of(distantDuplicate, secondCourt, thirdCourt, nearestDuplicate, laterDistantDuplicate));

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.DEFAULT_AOL_DISTANCE,
            SearchAction.NEAREST,
            2
        );

        assertThat(response).containsExactly(nearestDuplicate, secondCourt);
    }

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
            area.getId(),
            51.5,
            -0.1,
            "SW1A1",
            "SW1A",
            "SW",
            5
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
            area.getId(),
            51.5,
            -0.1,
            "SW1A1",
            "SW1A",
            "SW",
            5
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
    void executeSearchStrategyShouldReturnNoCourtResultsForFamilyRegionalServiceCentreRouting() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEmpty();
        verify(courtAddressRepository, never()).findNearestByAreaOfLaw(anyDouble(), anyDouble(), any(), anyInt());
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
    void executeSearchStrategyShouldFallbackToNearestWhenLocalAuthoritySearchReturnsNoRows() {
        ServiceArea area = serviceArea(ServiceAreaType.FAMILY);
        OsLocationData locationData = osLocationData("Authority", "SW1A 1AA");
        LocalAuthorityType authorityType = localAuthorityType(UUID.randomUUID());
        List<CourtWithDistance> nearestResults = List.of(mock(CourtWithDistance.class));

        when(localAuthorityTypeRepository.findIdByNameIgnoreCase("Authority"))
            .thenReturn(Optional.of(authorityType));
        when(courtAddressRepository.findFamilyNonRegionalByLocalAuthority(
            51.5,
            -0.1,
            area.getAreaOfLawId(),
            authorityType.getId(),
            10
        )).thenReturn(List.of());
        when(courtAddressRepository.findNearestByAreaOfLaw(51.5, -0.1, area.getAreaOfLawId(), 10))
            .thenReturn(nearestResults);

        List<CourtWithDistance> response = searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.FAMILY_NON_REGIONAL,
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(response).isEqualTo(nearestResults);
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

    private CourtWithDistance courtWithDistance(UUID courtId, String distance) {
        CourtWithDistance result = mock(CourtWithDistance.class);
        when(result.getCourtId()).thenReturn(courtId);
        when(result.getDistance()).thenReturn(new BigDecimal(distance));
        return result;
    }
}
