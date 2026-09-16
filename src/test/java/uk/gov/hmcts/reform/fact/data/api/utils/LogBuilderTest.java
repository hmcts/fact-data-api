package uk.gov.hmcts.reform.fact.data.api.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.fact.data.api.utils.LogBuilder.writeLog;


class LogBuilderTest {

    private static final UUID USER_UUID = UUID.randomUUID();
    private static final String ACTION_VALUE = "fact-log-action";

    @Test
    void shouldWriteLogWithUserAndActionValue() {
        final String logMessage = writeLog(USER_UUID, ACTION_VALUE);

        assertTrue(
            logMessage.startsWith(String.format("Track: %s, %s, at ", USER_UUID, ACTION_VALUE)),
            "Log message should include user id, action value and the 'at' timestamp marker"
        );
    }

    @Test
    void shouldWriteLogWithActionValueOnly() {
        final String logMessage = writeLog(ACTION_VALUE);

        assertTrue(
            logMessage.startsWith(String.format("Track: %s, at ", ACTION_VALUE)),
            "Log message should include action value and the 'at' timestamp marker"
        );
    }

    @Test
    void shouldWriteLogWithActionAndDetails() {
        final String logMessage = writeLog("Upload CSV", "courtId=123", "status=FAILED");

        assertTrue(
            logMessage.startsWith("Track: Upload CSV, details: [courtId=123, status=FAILED], at "),
            "Log message should include action, details list and the 'at' timestamp marker"
        );
    }

    @Test
    void shouldWriteLogWithActionAndNoDetails() {
        final String logMessage = writeLog("Health check", (Object[]) new Object[]{});

        assertTrue(
            logMessage.startsWith("Track: Health check, details: [], at "),
            "Varargs overload should support empty details"
        );
    }
}
