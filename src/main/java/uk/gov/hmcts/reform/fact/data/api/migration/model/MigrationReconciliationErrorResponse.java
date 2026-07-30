package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationReconciliationErrorResponse {
    private String message;
    private LocalDateTime timestamp;
    private UUID reportId;
    private int errorCount;
}
