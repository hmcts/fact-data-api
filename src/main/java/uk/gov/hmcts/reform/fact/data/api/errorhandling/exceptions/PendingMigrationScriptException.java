package uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions;

public class PendingMigrationScriptException extends RuntimeException {

    public PendingMigrationScriptException(String script) {
        super("Found a migration not yet applied: " + script);
    }
}
