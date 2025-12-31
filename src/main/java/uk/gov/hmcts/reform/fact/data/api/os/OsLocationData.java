package uk.gov.hmcts.reform.fact.data.api.os;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OsLocationData {
    private String postcode;
    private String authorityName;
    private double latitude;
    private double longitude;
}
