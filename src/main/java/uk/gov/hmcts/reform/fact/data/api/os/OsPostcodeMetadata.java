package uk.gov.hmcts.reform.fact.data.api.os;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsPostcodeMetadata {
    private boolean largeUser;
    private boolean terminated;
}
