package uk.gov.hmcts.reform.fact.data.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "postcode_reference")
public class PostcodeReference {

    @Id
    @Column(length = 8)
    private String postcode;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "is_large_user", nullable = false)
    private boolean largeUser;

    @Column(name = "introduced_yyyymm", nullable = false, length = 6)
    private String introducedYearMonth;

    @Column(name = "terminated_yyyymm", length = 6)
    private String terminatedYearMonth;

    @Column(name = "coordinate_quality", nullable = false)
    private int coordinateQuality;

    @Column(name = "has_coordinates", nullable = false)
    private boolean coordinatesAvailable;

    private Double latitude;

    private Double longitude;

    public boolean isTerminated() {
        return "terminated".equals(status);
    }
}
