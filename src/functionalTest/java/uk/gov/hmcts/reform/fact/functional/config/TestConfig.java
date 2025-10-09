package uk.gov.hmcts.reform.fact.functional.config;

import java.util.Optional;

public final class TestConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8989";
    private static final String PROP_BASE_URL = "test.baseUrl";

    private final String baseUrl;

    private TestConfig() {
        this.baseUrl = read(PROP_BASE_URL).orElse(DEFAULT_BASE_URL);
    }

    public static TestConfig load() {
        return new TestConfig();
    }

    public String baseUrl() {
        return baseUrl;
    }

    private static Optional<String> read(final String key) {
        final var sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return Optional.of(sys);
        }
        final var envKey = key.replace('.', '_').toUpperCase();
        final var env = System.getenv(envKey);
        return (env != null && !env.isBlank()) ? Optional.of(env) : Optional.empty();
    }
}
