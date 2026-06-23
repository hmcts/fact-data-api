package uk.gov.hmcts.reform.fact.data.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class ServiceAreaSearchResult {
    private UUID id;
    private UUID serviceCentreId;
    private String serviceCentreName;
    private String serviceCentreSlug;
    private List<UUID> serviceAreaIds;
    private CatchmentType catchmentType;
    private SearchResultType type;
}
