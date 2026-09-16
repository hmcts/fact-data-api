package uk.gov.hmcts.reform.fact.data.api.utils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LogBuilder {

    private LogBuilder() {

    }

    public static String writeLog(String actionValue) {
        return String.format("Track: %s, at %s", actionValue, LocalDateTime.now());
    }

    public static String writeLog(String action, Object... values) {
        String details = Arrays.stream(values)
            .map(String::valueOf)
            .collect(Collectors.joining(", "));

        return String.format("Track: %s, details: [%s], at %s", action, details, LocalDateTime.now());
    }

    public static String writeLog(UUID actioningUserId, String actionValue) {
        return String.format("Track: %s, %s, at %s", actioningUserId, actionValue, LocalDateTime.now());
    }
}
