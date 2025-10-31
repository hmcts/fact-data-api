package uk.gov.hmcts.reform.fact.data.api.migration.exception;

public class MigrationClientException extends RuntimeException {

    public MigrationClientException(String message) {
        super(message);
    }

    public MigrationClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
