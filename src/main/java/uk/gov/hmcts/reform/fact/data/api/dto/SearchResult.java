package uk.gov.hmcts.reform.fact.data.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class SearchResult {

    private UUID id;
    private String name;
    private String slug;
    private BigDecimal distance;
    private SearchResultType type;

    public static SearchResult fromCourt(CourtWithDistance court) {
        return SearchResult.builder()
            .id(court.getCourtId())
            .name(court.getCourtName())
            .slug(court.getCourtSlug())
            .distance(court.getDistance())
            .type(SearchResultType.COURT)
            .build();
    }

    public static SearchResult fromServiceCentre(ServiceCentreWithDistance serviceCentre) {
        return SearchResult.builder()
            .id(serviceCentre.getServiceCentreId())
            .name(serviceCentre.getServiceCentreName())
            .slug(serviceCentre.getServiceCentreSlug())
            .distance(serviceCentre.getDistance())
            .type(SearchResultType.SERVICE_CENTRE)
            .build();
    }
}
