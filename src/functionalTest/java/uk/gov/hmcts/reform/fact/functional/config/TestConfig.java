package uk.gov.hmcts.reform.fact.functional.config;

public final class TestConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8989";

    private final String baseUrl;

    private TestConfig() {
        this.baseUrl = System.getenv().getOrDefault("TEST_URL", DEFAULT_BASE_URL);
    }

    public static TestConfig load() {
        return new TestConfig();
    }

    public String baseUrl() {
        return baseUrl;
    }

}
