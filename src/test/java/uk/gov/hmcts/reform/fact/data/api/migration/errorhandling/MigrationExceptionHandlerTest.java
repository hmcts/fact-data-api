package uk.gov.hmcts.reform.fact.data.api.migration.errorhandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationReconciliationException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingCounts;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationReconciliationErrorResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationReport;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResult;

class MigrationExceptionHandlerTest {

    private final MigrationExceptionHandler handler = new MigrationExceptionHandler();

    @Test
    void shouldExposeReportIdentityForReconciliationFailure() {
        UUID reportId = UUID.randomUUID();
        MigrationResult result = new MigrationResult();
        result.setFindingCounts(new MigrationFindingCounts(3, 1, 0));
        MigrationReport report = MigrationReport.builder()
            .id(reportId)
            .result(result)
            .build();

        MigrationReconciliationErrorResponse response =
            handler.handle(new MigrationReconciliationException(report));

        assertThat(response.getReportId()).isEqualTo(reportId);
        assertThat(response.getErrorCount()).isEqualTo(3);
        assertThat(response.getMessage()).contains("3 error finding(s)");
    }
}
