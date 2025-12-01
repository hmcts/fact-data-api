package uk.gov.hmcts.reform.fact.data.api.os;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OsLocationData {

    private String postcode;
    private String authorityName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
