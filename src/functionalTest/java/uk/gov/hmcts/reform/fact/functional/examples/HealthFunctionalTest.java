package uk.gov.hmcts.reform.fact.functional.examples;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.functional.config.TestConfig;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

@Disabled
public final class HealthFunctionalTest {

    private static final String HEALTH_ENDPOINT = "/health";
    private static HttpClient http;

    @BeforeAll
    static void setUp() {
        final var config = TestConfig.load();
        http = new HttpClient(config);
    }

    @Test
    void shouldReturnOkFromHealth() {
        final Response response = http.doGet(HEALTH_ENDPOINT);
        assertThat(response.statusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldFailOnPost() {
        final Response response = http.doPost(HEALTH_ENDPOINT, "");
        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    void shouldReturn404ForNonExistentEndpoint() {
        final var response = http.doGet("/this-does-not-exist");
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldReturnJsonContentType() {
        final var response = http.doGet(HEALTH_ENDPOINT);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).contains("json");
    }

    @Test
    void shouldReturnResponseWithinReasonableTime() {
        final var startTime = System.currentTimeMillis();
        final var response = http.doGet(HEALTH_ENDPOINT);
        final var duration = System.currentTimeMillis() - startTime;

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(duration).isLessThan(5000);
    }
}
