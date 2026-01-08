package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchExecuter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCourtServiceTest {

    private static final String CHILDCARE_SERVICE_AREA = "Childcare arrangements if you separate from your partner";

    @Mock
    private OsService osService;

    @Mock
    private ServiceAreaService serviceAreaService;

    @Mock
    private CourtSinglePointOfEntryService courtSinglePointOfEntryService;

    @Mock
    private CourtServiceAreaService courtServiceAreaService;

    @Mock
    private CourtAddressService courtAddressService;

    @Mock
    private SearchExecuter searchExecuter;

    @InjectMocks
    private SearchCourtService searchCourtService;

    @Test
    void getCourtsBySearchParametersShouldThrowWhenOnlyServiceAreaProvided() {
        assertThatThrownBy(() -> searchCourtService.getCourtsBySearchParameters(
            "SW1A 1AA",
            "Civil",
            null,
            10
        )).isInstanceOf(InvalidParameterCombinationException.class)
            .hasMessageContaining("Both 'serviceArea' and 'action' must be provided together");
    }

    @Test
    void getCourtsBySearchParametersShouldThrowWhenOnlyActionProvided() {
        assertThatThrownBy(() -> searchCourtService.getCourtsBySearchParameters(
            "SW1A 1AA",
            " ",
            SearchAction.NEAREST,
            10
        )).isInstanceOf(InvalidParameterCombinationException.class)
            .hasMessageContaining("Both 'serviceArea' and 'action' must be provided together");
    }

    @Test
    void getCourtsBySearchParametersShouldSearchPostcodeOnlyWhenServiceAreaMissing() {
        OsData osData = osDataWithLatLon(51.5, -0.1);
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(osService.getOsAddressByFullPostcode("SW1A 1AA")).thenReturn(osData);
        when(courtAddressService.findCourtWithDistanceByOsData(51.5, -0.1, 10)).thenReturn(results);

        List<CourtWithDistance> response = searchCourtService.getCourtsBySearchParameters(
            "SW1A 1AA",
            null,
            null,
            10
        );

        assertThat(response).isEqualTo(results);
        verify(osService).getOsAddressByFullPostcode("SW1A 1AA");
        verify(courtAddressService).findCourtWithDistanceByOsData(51.5, -0.1, 10);
    }

    @Test
    void searchWithServiceAreaShouldDelegateToSpoeForChildcare() {
        OsLocationData locationData = OsLocationData.builder()
            .latitude(51.5)
            .longitude(-0.1)
            .authorityName("Authority")
            .postcode("SW1A 1")
            .build();
        ServiceArea area = serviceAreaWithType(ServiceAreaType.FAMILY);
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(osService.getOsLonLatDistrictByPartial("SW1A 1AA")).thenReturn(locationData);
        when(serviceAreaService.getServiceAreaByName(CHILDCARE_SERVICE_AREA)).thenReturn(area);
        when(courtSinglePointOfEntryService.getChildcareCourtsSpoe(51.5, -0.1)).thenReturn(results);

        List<CourtWithDistance> response = searchCourtService.searchWithServiceArea(
            "SW1A 1AA",
            CHILDCARE_SERVICE_AREA,
            SearchAction.DOCUMENTS,
            5
        );

        assertThat(response).isEqualTo(results);
        verify(courtSinglePointOfEntryService).getChildcareCourtsSpoe(51.5, -0.1);
        verify(searchExecuter, never()).executeSearchStrategy(any(), any(), any(), any(), anyInt());
    }

    @Test
    void searchWithServiceAreaShouldUseSelectedStrategy() {
        OsLocationData locationData = OsLocationData.builder()
            .latitude(51.5)
            .longitude(-0.1)
            .authorityName("Authority")
            .postcode("SW1A 1")
            .build();
        ServiceArea area = serviceAreaWithType(ServiceAreaType.CIVIL);
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));

        when(osService.getOsLonLatDistrictByPartial("SW1A 1AA")).thenReturn(locationData);
        when(serviceAreaService.getServiceAreaByName("Money Claims")).thenReturn(area);
        when(searchExecuter.executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.CIVIL_POSTCODE_PREFERENCE,
            SearchAction.DOCUMENTS,
            5
        )).thenReturn(results);

        List<CourtWithDistance> response = searchCourtService.searchWithServiceArea(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.DOCUMENTS,
            5
        );

        assertThat(response).isEqualTo(results);
        verify(searchExecuter).executeSearchStrategy(
            locationData,
            area,
            SearchStrategy.CIVIL_POSTCODE_PREFERENCE,
            SearchAction.DOCUMENTS,
            5
        );
    }

    @Test
    void selectSearchStrategyShouldReturnDefaultForNearest() {
        ServiceArea area = serviceAreaWithType(ServiceAreaType.CIVIL);

        SearchStrategy strategy = searchCourtService.selectSearchStrategy(
            SearchAction.NEAREST,
            "Authority",
            area
        );

        assertThat(strategy).isEqualTo(SearchStrategy.DEFAULT_AOL_DISTANCE);
    }

    @Test
    void selectSearchStrategyShouldReturnCivilPreference() {
        ServiceArea area = serviceAreaWithType(ServiceAreaType.CIVIL);

        SearchStrategy strategy = searchCourtService.selectSearchStrategy(
            SearchAction.DOCUMENTS,
            "Authority",
            area
        );

        assertThat(strategy).isEqualTo(SearchStrategy.CIVIL_POSTCODE_PREFERENCE);
    }

    @Test
    void selectSearchStrategyShouldReturnFamilyRegionalWhenRegionalCatchmentExists() {
        ServiceArea area = serviceAreaWithType(ServiceAreaType.FAMILY);
        area.setCatchmentMethod(CatchmentMethod.LOCAL_AUTHORITY);
        CourtServiceAreas courtServiceAreas = new CourtServiceAreas();
        courtServiceAreas.setCatchmentType(CatchmentType.REGIONAL);

        when(courtServiceAreaService.findByServiceAreaId(area.getId()))
            .thenReturn(List.of(courtServiceAreas));

        SearchStrategy strategy = searchCourtService.selectSearchStrategy(
            SearchAction.DOCUMENTS,
            "Authority",
            area
        );

        assertThat(strategy).isEqualTo(SearchStrategy.FAMILY_REGIONAL);
    }

    @Test
    void selectSearchStrategyShouldReturnFamilyNonRegionalWhenNoRegionalCatchment() {
        ServiceArea area = serviceAreaWithType(ServiceAreaType.FAMILY);
        area.setCatchmentMethod(CatchmentMethod.LOCAL_AUTHORITY);
        CourtServiceAreas courtServiceAreas = new CourtServiceAreas();
        courtServiceAreas.setCatchmentType(CatchmentType.LOCAL);

        when(courtServiceAreaService.findByServiceAreaId(area.getId()))
            .thenReturn(List.of(courtServiceAreas));

        SearchStrategy strategy = searchCourtService.selectSearchStrategy(
            SearchAction.DOCUMENTS,
            "Authority",
            area
        );

        assertThat(strategy).isEqualTo(SearchStrategy.FAMILY_NON_REGIONAL);
    }

    @Test
    void selectSearchStrategyShouldReturnDefaultWhenAuthorityMissing() {
        ServiceArea area = serviceAreaWithType(ServiceAreaType.FAMILY);
        area.setCatchmentMethod(CatchmentMethod.LOCAL_AUTHORITY);

        SearchStrategy strategy = searchCourtService.selectSearchStrategy(
            SearchAction.DOCUMENTS,
            "",
            area
        );

        assertThat(strategy).isEqualTo(SearchStrategy.DEFAULT_AOL_DISTANCE);
    }

    private OsData osDataWithLatLon(double lat, double lon) {
        OsResult result = OsResult.builder()
            .dpa(OsDpa.builder()
                .lat(lat)
                .lng(lon)
                .build())
            .build();

        return OsData.builder()
            .results(List.of(result))
            .build();
    }

    private ServiceArea serviceAreaWithType(ServiceAreaType type) {
        ServiceArea area = new ServiceArea();
        area.setId(UUID.randomUUID());
        area.setAreaOfLawId(UUID.randomUUID());
        area.setType(type);
        return area;
    }
}
