package uk.gov.hmcts.reform.fact.data.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for deserializing {@link CourtWithDistance} JSON responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class CourtWithDistanceResponse implements CourtWithDistance {

    private UUID courtId;
    private String courtName;
    private String courtSlug;
    private BigDecimal distance;

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
