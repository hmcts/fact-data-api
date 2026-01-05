package uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomExceptionsTest {

    private static final String TEST_MESSAGE = "This is a test message";
    private static final String ASSERTION_MESSAGE = "The message should match the message passed in";

    @Test
    void testCreationOfNotFoundException() {
        NotFoundException exception = new NotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, exception.getMessage(), ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfTranslationNotFoundException() {
        CourtResourceNotFoundException exception = new CourtResourceNotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, exception.getMessage(), ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfPendingMigrationScriptException() {
        String scriptName = "V1__initial.sql";
        PendingMigrationScriptException exception = new PendingMigrationScriptException(scriptName);
        assertEquals("Found a migration not yet applied: " + scriptName,
                     exception.getMessage(),
                     ASSERTION_MESSAGE);
    }
}
