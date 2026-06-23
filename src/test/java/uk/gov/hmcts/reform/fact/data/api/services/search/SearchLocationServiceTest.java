package uk.gov.hmcts.reform.fact.data.api.services.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceCentreWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchLocationServiceTest {

    @Mock
    private SearchCourtService searchCourtService;

    @Mock
    private SearchServiceCentreService searchServiceCentreService;

    @InjectMocks
    private SearchLocationService searchLocationService;

    @Test
    void getLocationsBySearchParametersReturnsCourtsAndServiceCentresSortedByDistance() {
        UUID courtId = UUID.randomUUID();
        UUID serviceCentreId = UUID.randomUUID();
        CourtWithDistance court = courtWithDistance(courtId, BigDecimal.valueOf(3));
        ServiceCentreWithDistance serviceCentre =
            serviceCentreWithDistance(serviceCentreId, BigDecimal.valueOf(1));

        when(searchCourtService.getCourtsBySearchParameters("SW1A 1AA", "Money Claims", SearchAction.DOCUMENTS, 10))
            .thenReturn(List.of(court));
        when(searchServiceCentreService.getServiceCentresBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.DOCUMENTS,
            10
        )).thenReturn(List.of(serviceCentre));

        List<SearchResult> results = searchLocationService.getLocationsBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.DOCUMENTS,
            10
        );

        assertThat(results).extracting(SearchResult::getType)
            .containsExactly(SearchResultType.SERVICE_CENTRE, SearchResultType.COURT);
        assertThat(results).extracting(SearchResult::getId)
            .containsExactly(serviceCentreId, courtId);
    }

    private CourtWithDistance courtWithDistance(UUID courtId, BigDecimal distance) {
        return new CourtWithDistance() {
            @Override
            public UUID getCourtId() {
                return courtId;
            }

            @Override
            public String getCourtName() {
                return "Test Court";
            }

            @Override
            public String getCourtSlug() {
                return "test-court";
            }

            @Override
            public BigDecimal getDistance() {
                return distance;
            }
        };
    }

    private ServiceCentreWithDistance serviceCentreWithDistance(UUID serviceCentreId, BigDecimal distance) {
        return new ServiceCentreWithDistance() {
            @Override
            public UUID getServiceCentreId() {
                return serviceCentreId;
            }

            @Override
            public String getServiceCentreName() {
                return "Test Service Centre";
            }

            @Override
            public String getServiceCentreSlug() {
                return "test-service-centre";
            }

            @Override
            public BigDecimal getDistance() {
                return distance;
            }
        };
    }
}
