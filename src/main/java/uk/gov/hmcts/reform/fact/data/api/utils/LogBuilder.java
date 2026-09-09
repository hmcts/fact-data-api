package uk.gov.hmcts.reform.fact.data.api.utils;

import java.time.LocalDateTime;
import java.util.UUID;

public final class LogBuilder {

    private LogBuilder() {

    }

    public static String writeLog(String actionValue) {
        return String.format("Track: %s, at %s", actionValue, LocalDateTime.now());
    }

    public static String writeLog(UUID actioningUserId, String actionValue) {
        return String.format("Track: %s, %s, at %s", actioningUserId, actionValue, LocalDateTime.now());
    }
}
