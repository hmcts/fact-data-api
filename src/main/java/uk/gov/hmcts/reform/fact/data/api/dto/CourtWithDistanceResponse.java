package uk.gov.hmcts.reform.fact.data.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for deserializing {@link CourtWithDistance} JSON responses.
 *
 * @param courtId the court ID
 * @param courtName the court name
 * @param courtSlug the court slug
 * @param distance the distance from the searched point
 */
public record CourtWithDistanceResponse(
    UUID courtId,
    String courtName,
    String courtSlug,
    BigDecimal distance
) implements CourtWithDistance {

    @Override
    public UUID getCourtId() {
        return courtId;
    }

    @Override
    public String getCourtName() {
        return courtName;
    }

    @Override
    public String getCourtSlug() {
        return courtSlug;
    }

    @Override
    public BigDecimal getDistance() {
        return distance;
    }
}
