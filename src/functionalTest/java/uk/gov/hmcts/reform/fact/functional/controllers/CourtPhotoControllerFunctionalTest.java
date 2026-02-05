package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

@Feature("Court Photo Controller")
@DisplayName("Court Photo Controller")
public final class CourtPhotoControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final List<UUID> courtsWithPhotos = new ArrayList<>();

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo uploads valid JPEG")
    void shouldUploadValidJpegPhoto() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Upload JPEG");
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
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Upload PNG");
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
    @DisplayName("POST /courts/{courtId}/v1/photo fails with non-existent court ID")
    void shouldFailToUploadPhotoToNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final File testImage = new File("src/functionalTest/resources/test-images/test valid jpg 1.2 MB.jpg");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + nonExistentCourtId + "/v1/photo",
            "file",
            testImage
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(uploadResponse.jsonPath().getString("message"))
            .contains("Court not found");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo fails with invalid file format")
    void shouldFailToUploadInvalidFileFormat() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Invalid Format");
        courtsWithPhotos.add(courtId);

        final File invalidFile = new File(
            "src/functionalTest/resources/test-images/test invalid format file.txt");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + courtId + "/v1/photo",
            "file",
            invalidFile
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(uploadResponse.jsonPath().getString("message"))
            .contains("Only JPEG or PNG files are allowed");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/photo fails with file too large")
    void shouldFailToUploadOversizedFile() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Oversized");
        courtsWithPhotos.add(courtId);

        final File oversizedFile = new File(
            "src/functionalTest/resources/test-images/test invalid jpg 3.2 MB.jpg");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + courtId + "/v1/photo",
            "file",
            oversizedFile
        );

        assertThat(uploadResponse.statusCode())
            .as("Expected 413 when uploading an oversized photo")
            .isEqualTo(PAYLOAD_TOO_LARGE.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/photo retrieves photo metadata")
    void shouldRetrievePhotoMetadata() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Get Metadata");
        courtsWithPhotos.add(courtId);

        final File testImage = new File("src/functionalTest/resources/test-images/test valid jpg 1.2 MB.jpg");
        final Response uploadResponse = http.doMultipartPost(
            "/courts/" + courtId + "/v1/photo",
            "file",
            testImage
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(CREATED.value());
        final CourtPhoto uploadedPhoto = mapper.readValue(uploadResponse.getBody().asString(), CourtPhoto.class);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/photo");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtPhoto retrievedPhoto = mapper.readValue(getResponse.getBody().asString(), CourtPhoto.class);
        assertThat(retrievedPhoto.getId()).isEqualTo(uploadedPhoto.getId());
        assertThat(retrievedPhoto.getCourtId()).isEqualTo(courtId);
        assertThat(retrievedPhoto.getFileLink()).isEqualTo(uploadedPhoto.getFileLink())
            .contains("blob.core.windows.net").contains(courtId.toString());
        assertThat(retrievedPhoto.getLastUpdatedAt()).isEqualTo(uploadedPhoto.getLastUpdatedAt());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/photo fails when no photo exists")
    void shouldFailToRetrieveNonExistentPhoto() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo No Photo");
        courtsWithPhotos.add(courtId);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/photo");

        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Court photo not found for court ID: " + courtId);
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/photo deletes existing photo")
    void shouldDeleteExistingPhoto() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Delete");
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

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/photo fails when no photo exists")
    void shouldFailToDeleteNonExistentPhoto() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Photo Delete No Photo");
        courtsWithPhotos.add(courtId);

        final Response deleteResponse = http.doDelete("/courts/" + courtId + "/v1/photo");

        assertThat(deleteResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(deleteResponse.jsonPath().getString("message"))
            .contains("Court photo not found for court ID: " + courtId);
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
