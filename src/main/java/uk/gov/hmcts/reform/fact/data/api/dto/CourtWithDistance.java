package uk.gov.hmcts.reform.fact.data.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface CourtWithDistance {
    /**
     * Returns the court identifier.
     *
     * @return the court id
     */
    UUID getCourtId();

    /**
     * Returns the court name.
     *
     * @return the court name
     */
    String getCourtName();

    /**
     * Returns the court slug.
     *
     * @return the court slug
     */
    String getCourtSlug();

    /**
     * Returns the distance value for the court.
     *
     * @return the distance value
     */
    BigDecimal getDistance();
}
