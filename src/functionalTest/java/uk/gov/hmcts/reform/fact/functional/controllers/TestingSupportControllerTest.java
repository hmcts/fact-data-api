package uk.gov.hmcts.reform.fact.functional.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

@Feature("Testing Support Controller Controller")
@DisplayName("Testing Support Controller Controller")
@Slf4j
public class TestingSupportControllerTest {
    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @ParameterizedTest
    @DisplayName("Test random court generation works")
    @ValueSource(strings = {
        "TSC Auth Test A", "TSC Auth Test B", "TSC Auth Test C", "TSC Auth Test D", "TSC Auth Test E",
        "TSC Auth Test F", "TSC Auth Test G", "TSC Auth Test H", "TSC Auth Test I", "TSC Auth Test J"
    })
    void testingSupportEndpointAuth(String courtName) throws JsonProcessingException {
        String endpoint = "/testing-support/courts?courtName=" + courtName;
        Response response = http.doGet(endpoint);

        assertThat(response).isNotNull();
        AssertionHelper.assertStatus(response, HttpStatus.CREATED);

        CourtDetails createdCourt = mapper.readValue(response.getBody().asString(), CourtDetails.class);
        assertThat(createdCourt.getSlug()).isNotNull();

        endpoint = "/courts/" + createdCourt.getId() + "/v1";
        response = http.doGet(endpoint);
        assertThat(response).isNotNull();
        AssertionHelper.assertStatus(response, HttpStatus.OK);

        CourtDetails retrievedCourt = mapper.readValue(response.getBody().asString(), CourtDetails.class);
        log.info(response.getBody().asString());

        // make sure that expected data is there (everything else is optional)
        assertThat(retrievedCourt.getId()).isEqualTo(createdCourt.getId());
        assertThat(retrievedCourt.getOpen()).isTrue();
        assertThat(retrievedCourt.getCourtAddresses()).isNotEmpty();
        assertThat(retrievedCourt.getCourtAccessibilityOptions()).isNotEmpty();
        assertThat(retrievedCourt.getCourtContactDetails()).isNotEmpty();
        assertThat(retrievedCourt.getCourtAreasOfLaw()).isNotEmpty();
        assertThat(retrievedCourt.getCourtOpeningHours()).isNotEmpty();
        assertThat(retrievedCourt.getCourtCounterServiceOpeningHours()).isNotEmpty();
        assertThat(retrievedCourt.getCourtFacilities()).isNotEmpty();
        assertThat(retrievedCourt.getCourtProfessionalInformation()).isNotEmpty();
        assertThat(retrievedCourt.getCourtPhotos()).isNotEmpty();
    }

    @AfterAll
    static void cleanUp() {
        http.doDelete("/testing-support/courts/name-prefix/TSC Auth Test");
    }
}
