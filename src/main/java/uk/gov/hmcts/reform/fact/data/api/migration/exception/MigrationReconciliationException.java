package uk.gov.hmcts.reform.fact.data.api.migration.exception;

import lombok.Getter;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationReport;

@Getter
public class MigrationReconciliationException extends RuntimeException {

    private final transient MigrationReport report;

    public MigrationReconciliationException(MigrationReport report) {
        super("Migration reconciliation failed with "
            + report.getResult().getFindingCounts().getErrors() + " error finding(s)");
        this.report = report;
    }
}
