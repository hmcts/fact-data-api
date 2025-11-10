package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Photo Controller")
@DisplayName("Court Photo Controller")
public final class CourtPhotoControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String regionId = TestDataHelper.getRegionId(http);
    private static final List<UUID> courtsWithPhotos = new ArrayList<>();

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo uploads valid JPEG")
    void shouldUploadValidJpegPhoto() throws Exception {
        final Court court = new Court();
        court.setName("Test Court Photo Upload JPEG");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));
        courtsWithPhotos.add(courtId);

        final File testImage = new File("src/functionalTest/resources/test-images/test valid jpg 1.2 MB.jpg");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + courtId + "/v1/photo",
            "file",
            testImage
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(CREATED.value());

        final CourtPhoto photo = mapper.readValue(uploadResponse.getBody().asString(), CourtPhoto.class);
        assertThat(photo.getId()).isNotNull();
        assertThat(photo.getCourtId()).isEqualTo(courtId);
        assertThat(photo.getFileLink()).isNotNull().contains("blob.core.windows.net").contains(courtId.toString());
        assertThat(photo.getLastUpdatedAt()).isNotNull();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/photo");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtPhoto fetchedPhoto = mapper.readValue(getResponse.getBody().asString(), CourtPhoto.class);
        assertThat(fetchedPhoto.getId()).isEqualTo(photo.getId());
        assertThat(fetchedPhoto.getFileLink()).isEqualTo(photo.getFileLink());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo uploads valid PNG")
    void shouldUploadValidPngPhoto() throws Exception {
        final Court court = new Court();
        court.setName("Test Court Photo Upload PNG");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));
        courtsWithPhotos.add(courtId);

        final File testImage = new File("src/functionalTest/resources/test-images/test valid png 1.4 MB.png");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + courtId + "/v1/photo",
            "file",
            testImage
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(CREATED.value());

        final CourtPhoto photo = mapper.readValue(uploadResponse.getBody().asString(), CourtPhoto.class);
        assertThat(photo.getId()).isNotNull();
        assertThat(photo.getCourtId()).isEqualTo(courtId);
        assertThat(photo.getFileLink()).isNotNull().contains("blob.core.windows.net").contains(courtId.toString());
        assertThat(photo.getLastUpdatedAt()).isNotNull();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/photo");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtPhoto fetchedPhoto = mapper.readValue(getResponse.getBody().asString(), CourtPhoto.class);
        assertThat(fetchedPhoto.getId()).isEqualTo(photo.getId());
        assertThat(fetchedPhoto.getFileLink()).isEqualTo(photo.getFileLink());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/photo deletes existing photo")
    void shouldDeleteExistingPhoto() throws Exception {
        final Court court = new Court();
        court.setName("Test Court Photo Delete");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));
        courtsWithPhotos.add(courtId);

        final File testImage = new File("src/functionalTest/resources/test-images/test valid jpg 1.2 MB.jpg");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + courtId + "/v1/photo",
            "file",
            testImage
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(CREATED.value());

        final Response deleteResponse = http.doDelete("/courts/" + courtId + "/v1/photo");
        assertThat(deleteResponse.statusCode()).isEqualTo(NO_CONTENT.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/photo");
        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
    }

    @AfterAll
    static void cleanUpTestData() {
        for (UUID courtId : courtsWithPhotos) {
            try {
                http.doDelete("/courts/" + courtId + "/v1/photo");
            } catch (Exception e) {
                // Photo might already be deleted in test, ignore
            }
        }

        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
