package uk.gov.hmcts.reform.fact.data.api.services.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceCentreWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceCentreServiceTest {

    @Mock
    private OsService osService;

    @Mock
    private ServiceAreaService serviceAreaService;

    @Mock
    private ServiceCentreRepository serviceCentreRepository;

    @InjectMocks
    private SearchServiceCentreService searchServiceCentreService;

    @Test
    void getServiceCentresBySearchParametersReturnsEmptyWhenSearchingPostcodeOnly() {
        List<ServiceCentreWithDistance> result = searchServiceCentreService.getServiceCentresBySearchParameters(
            "SW1A 1AA",
            null,
            null,
            10
        );

        assertThat(result).isEmpty();
        verifyNoInteractions(osService);
        verifyNoInteractions(serviceAreaService);
        verifyNoInteractions(serviceCentreRepository);
    }

    @Test
    void getServiceCentresBySearchParametersThrowsWhenOnlyServiceAreaProvided() {
        assertThatThrownBy(() -> searchServiceCentreService.getServiceCentresBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            null,
            10
        )).isInstanceOf(InvalidParameterCombinationException.class);
    }

    @Test
    void getServiceCentresBySearchParametersReturnsMatchingServiceCentres() {
        UUID serviceAreaId = UUID.randomUUID();
        UUID areaOfLawId = UUID.randomUUID();
        ServiceArea serviceArea = new ServiceArea();
        serviceArea.setId(serviceAreaId);
        serviceArea.setAreaOfLawId(areaOfLawId);
        OsLocationData osLocationData = OsLocationData.builder()
            .latitude(51.5)
            .longitude(-0.1)
            .build();
        List<ServiceCentreWithDistance> expected = List.of(mock(ServiceCentreWithDistance.class));

        when(osService.getOsLonLatDistrictByPartial("SW1A 1AA")).thenReturn(osLocationData);
        when(serviceAreaService.getServiceAreaByName("Money Claims")).thenReturn(serviceArea);
        when(serviceCentreRepository.findNearestByServiceAreaAndAreaOfLawAndCatchmentTypeIn(
            serviceAreaId,
            areaOfLawId,
            List.of(CatchmentType.LOCAL, CatchmentType.REGIONAL),
            51.5,
            -0.1,
            10
        )).thenReturn(expected);

        List<ServiceCentreWithDistance> result = searchServiceCentreService.getServiceCentresBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(result).isEqualTo(expected);
        verify(serviceCentreRepository).findNearestByServiceAreaAndAreaOfLawAndCatchmentTypeIn(
            serviceAreaId,
            areaOfLawId,
            List.of(CatchmentType.LOCAL, CatchmentType.REGIONAL),
            51.5,
            -0.1,
            10
        );
    }
}
