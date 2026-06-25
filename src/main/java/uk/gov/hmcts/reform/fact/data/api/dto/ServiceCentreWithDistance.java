package uk.gov.hmcts.reform.fact.data.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface ServiceCentreWithDistance {
    UUID getServiceCentreId();

    String getServiceCentreName();

    String getServiceCentreSlug();

    BigDecimal getDistance();
}
