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
}
