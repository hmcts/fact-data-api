package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSource {
    private String baseUrl;
    private Instant fetchedAt;
    private String payloadSha256;
}
