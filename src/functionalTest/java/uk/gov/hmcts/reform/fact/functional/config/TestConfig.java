package uk.gov.hmcts.reform.fact.functional.config;

public final class TestConfig {

    private final String baseUrl;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private TestConfig() {
        this.baseUrl = getRequiredEnv("TEST_URL");

        // Construct database URL from components
        final String dbHost = getRequiredEnv("DB_HOST");
        final String dbPort = getRequiredEnv("DB_PORT");
        final String dbName = getRequiredEnv("DB_NAME");
        this.dbUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

        this.dbUser = getRequiredEnv("DB_USER");
        this.dbPassword = getRequiredEnv("DB_PASSWORD");
    }

    public static TestConfig load() {
        return new TestConfig();
    }

    private String getRequiredEnv(final String name) {
        final String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required environment variable '" + name + "' is not set. "
                + "For local development, create a .env file or set environment variables."
            );
        }
        return value;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPassword() {
        return dbPassword;
    }

}
