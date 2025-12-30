package uk.gov.hmcts.reform.fact.data.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface CourtWithDistance {
    UUID getCourtId();
    String getCourtName();
    String getCourtSlug();
    BigDecimal getDistance();
}
