package uk.gov.hmcts.reform.fact.data.api.migration.exception;

public class MigrationAlreadyAppliedException extends RuntimeException {

    public MigrationAlreadyAppliedException(String message) {
        super(message);
    }
}
