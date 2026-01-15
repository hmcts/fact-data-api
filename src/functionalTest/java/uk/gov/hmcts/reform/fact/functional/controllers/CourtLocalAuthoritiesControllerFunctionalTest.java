package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Local Authorities Controller")
@DisplayName("Court Local Authorities Controller")
public final class CourtLocalAuthoritiesControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns 204 when no areas of law exist")
    void shouldReturn204WhenNoAreasOfLawExist() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Areas");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");

        assertThat(getResponse.statusCode())
            .as("Expected 204 when no local authorities exist for court %s", courtId)
            .isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns 404 for non-existent court")
    void shouldReturn404ForNonExistentCourtOnGet() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId + "/v1/local-authorities");

        assertThat(getResponse.statusCode())
            .as("Expected 404 for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .as("Error message should mention the missing court ID")
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/local-authorities returns data for court with enabled areas")
    void shouldReturnLocalAuthoritiesForCourtWithEnabledAreas() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Local Authorities");

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID childrenId = TestDataHelper.getAreaOfLawIdByName(http, "Children");
        final UUID civilPartnershipId = TestDataHelper.getAreaOfLawIdByName(http, "Civil partnership");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");

        final Response enableAreasResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                        TestDataHelper
                                                            .buildCourtAreasOfLaw(courtId, List
                                                                .of(adoptionId, childrenId, civilPartnershipId,
                                                                    divorceId)));
        assertThat(enableAreasResponse.statusCode())
            .as("Expected areas of law to be enabled for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(getResponse.statusCode())
            .as("Expected local authorities to be returned for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> localAuthorities = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        assertThat(localAuthorities)
            .as("Expected four local authority entries for the enabled areas of law")
            .hasSize(4);
        assertThat(localAuthorities).extracting("areaOfLawName")
            .as("Expected local authority entries for the enabled areas of law")
            .containsExactlyInAnyOrder("Adoption", "Children", "Civil partnership", "Divorce");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities sets local authority for single area")
    void shouldSetLocalAuthoritiesForSingleArea() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Set Local Authority");

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID childrenId = TestDataHelper.getAreaOfLawIdByName(http, "Children");
        final UUID civilPartnershipId = TestDataHelper.getAreaOfLawIdByName(http, "Civil partnership");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");

        final Response enableAreasResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                        TestDataHelper.buildCourtAreasOfLaw(courtId,
                                                            List.of(adoptionId, childrenId,
                                                                    civilPartnershipId, divorceId)));
        assertThat(enableAreasResponse.statusCode())
            .as("Expected areas of law to be enabled for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(initialGetResponse.statusCode())
            .as("Expected local authorities to be returned for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> initialAuthorities = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        final UUID firstLocalAuthorityId = initialAuthorities.get(0)
            .getLocalAuthorities().get(0).getId();

        final List<CourtLocalAuthorityDto> updates = List.of(
            buildCourtLocalAuthorityUpdate(adoptionId, List.of(buildLocalAuthoritySelection(
                firstLocalAuthorityId))),
            buildCourtLocalAuthorityUpdate(childrenId, List.of()),
            buildCourtLocalAuthorityUpdate(civilPartnershipId, List.of()),
            buildCourtLocalAuthorityUpdate(divorceId, List.of())
        );

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/local-authorities", updates);
        assertThat(putResponse.statusCode())
            .as("Expected local authorities to be updated for court %s", courtId)
            .isEqualTo(OK.value());

        final Response updatedGetResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(updatedGetResponse.statusCode())
            .as("Expected updated local authorities for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> updatedAuthorities = mapper.readValue(
            updatedGetResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        assertThat(updatedAuthorities)
            .filteredOn(dto -> "Adoption".equals(dto.getAreaOfLawName()))
            .singleElement()
            .satisfies(adoptionArea -> {
                assertThat(adoptionArea.getLocalAuthorities())
                    .as("Expected exactly one selected local authority for Adoption")
                    .filteredOn(LocalAuthoritySelectionDto::getSelected)
                    .hasSize(1);
                assertThat(adoptionArea.getLocalAuthorities())
                    .filteredOn(la -> la.getId().equals(firstLocalAuthorityId))
                    .singleElement()
                    .extracting(LocalAuthoritySelectionDto::getSelected)
                    .as("Selected local authority should remain selected after update")
                    .isEqualTo(true);
            });
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 404 for non-existent court")
    void shouldReturn404ForNonExistentCourtOnPut() {
        final UUID nonExistentCourtId = UUID.randomUUID();
        final String putBody = "[]";

        final Response putResponse = http.doPut("/courts/" + nonExistentCourtId
                                                    + "/v1/local-authorities", putBody);

        assertThat(putResponse.statusCode())
            .as("Expected 404 for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
        assertThat(putResponse.jsonPath().getString("message"))
            .as("Error message should mention the missing court ID")
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 204 when no areas of law exist")
    void shouldReturn204WhenNoAreasOfLawExistOnPut() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Areas Put");
        final String putBody = "[]";

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/local-authorities", putBody);

        assertThat(putResponse.statusCode())
            .as("Expected 204 when no areas of law exist for court %s", courtId)
            .isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities replaces existing selections")
    void shouldReplaceExistingLocalAuthorities() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Replace Local Authority");

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID childrenId = TestDataHelper.getAreaOfLawIdByName(http, "Children");
        final UUID civilPartnershipId = TestDataHelper.getAreaOfLawIdByName(http, "Civil partnership");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");

        final Response enableAreasResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                        TestDataHelper.buildCourtAreasOfLaw(courtId,
                                                            List.of(adoptionId, childrenId,
                                                                    civilPartnershipId, divorceId)));
        assertThat(enableAreasResponse.statusCode())
            .as("Expected areas of law to be enabled for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(initialGetResponse.statusCode())
            .as("Expected local authorities to be returned for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> initialAuthorities = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        final UUID firstLocalAuthorityId = initialAuthorities.get(0)
            .getLocalAuthorities().get(0).getId();
        final UUID secondLocalAuthorityId = initialAuthorities.get(0)
            .getLocalAuthorities().get(1).getId();

        final List<CourtLocalAuthorityDto> firstUpdates = List.of(
            buildCourtLocalAuthorityUpdate(adoptionId, List.of(buildLocalAuthoritySelection(
                firstLocalAuthorityId))),
            buildCourtLocalAuthorityUpdate(childrenId, List.of()),
            buildCourtLocalAuthorityUpdate(civilPartnershipId, List.of()),
            buildCourtLocalAuthorityUpdate(divorceId, List.of())
        );

        final Response firstPutResponse = http.doPut("/courts/" + courtId
                                                         + "/v1/local-authorities", firstUpdates);
        assertThat(firstPutResponse.statusCode())
            .as("Expected first local authority update to succeed for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> secondUpdates = List.of(
            buildCourtLocalAuthorityUpdate(adoptionId, List.of(buildLocalAuthoritySelection(
                secondLocalAuthorityId))),
            buildCourtLocalAuthorityUpdate(childrenId, List.of()),
            buildCourtLocalAuthorityUpdate(civilPartnershipId, List.of()),
            buildCourtLocalAuthorityUpdate(divorceId, List.of())
        );

        final Response secondPutResponse = http.doPut("/courts/" + courtId
                                                          + "/v1/local-authorities", secondUpdates);
        assertThat(secondPutResponse.statusCode())
            .as("Expected second local authority update to succeed for court %s", courtId)
            .isEqualTo(OK.value());

        final Response updatedGetResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(updatedGetResponse.statusCode())
            .as("Expected updated local authorities for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> updatedAuthorities = mapper.readValue(
            updatedGetResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        assertThat(updatedAuthorities)
            .filteredOn(dto -> "Adoption".equals(dto.getAreaOfLawName()))
            .singleElement()
            .satisfies(adoptionArea -> {
                assertThat(adoptionArea.getLocalAuthorities())
                    .as("Expected exactly one selected local authority after replacement")
                    .filteredOn(LocalAuthoritySelectionDto::getSelected)
                    .hasSize(1);
                assertThat(adoptionArea.getLocalAuthorities())
                    .filteredOn(la -> la.getId().equals(firstLocalAuthorityId))
                    .singleElement()
                    .extracting(LocalAuthoritySelectionDto::getSelected)
                    .as("First local authority should be deselected after replace")
                    .isEqualTo(false);
                assertThat(adoptionArea.getLocalAuthorities())
                    .filteredOn(la -> la.getId().equals(secondLocalAuthorityId))
                    .singleElement()
                    .extracting(LocalAuthoritySelectionDto::getSelected)
                    .as("Second local authority should be selected after replace")
                    .isEqualTo(true);
            });
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities clears selections with empty arrays")
    void shouldClearSelectionsWithEmptyArrays() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Clear Local Authority");

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID childrenId = TestDataHelper.getAreaOfLawIdByName(http, "Children");
        final UUID civilPartnershipId = TestDataHelper.getAreaOfLawIdByName(http, "Civil partnership");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");

        final Response enableAreasResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                        TestDataHelper.buildCourtAreasOfLaw(courtId,
                                                            List.of(adoptionId, childrenId,
                                                                    civilPartnershipId, divorceId)));
        assertThat(enableAreasResponse.statusCode())
            .as("Expected areas of law to be enabled for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(initialGetResponse.statusCode())
            .as("Expected local authorities to be returned for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> initialAuthorities = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        final UUID firstLocalAuthorityId = initialAuthorities.get(0)
            .getLocalAuthorities().get(0).getId();

        final List<CourtLocalAuthorityDto> firstUpdates = List.of(
            buildCourtLocalAuthorityUpdate(adoptionId, List.of(buildLocalAuthoritySelection(
                firstLocalAuthorityId))),
            buildCourtLocalAuthorityUpdate(childrenId, List.of()),
            buildCourtLocalAuthorityUpdate(civilPartnershipId, List.of()),
            buildCourtLocalAuthorityUpdate(divorceId, List.of())
        );

        final Response firstPutResponse = http.doPut("/courts/" + courtId
                                                         + "/v1/local-authorities", firstUpdates);
        assertThat(firstPutResponse.statusCode())
            .as("Expected initial local authority selection to succeed for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> clearUpdates = List.of(
            buildCourtLocalAuthorityUpdate(adoptionId, List.of()),
            buildCourtLocalAuthorityUpdate(childrenId, List.of()),
            buildCourtLocalAuthorityUpdate(civilPartnershipId, List.of()),
            buildCourtLocalAuthorityUpdate(divorceId, List.of())
        );

        final Response clearPutResponse = http.doPut("/courts/" + courtId
                                                         + "/v1/local-authorities", clearUpdates);
        assertThat(clearPutResponse.statusCode())
            .as("Expected local authority selections to be cleared for court %s", courtId)
            .isEqualTo(OK.value());

        final Response updatedGetResponse = http.doGet("/courts/" + courtId + "/v1/local-authorities");
        assertThat(updatedGetResponse.statusCode())
            .as("Expected updated local authorities for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> updatedAuthorities = mapper.readValue(
            updatedGetResponse.asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        assertThat(updatedAuthorities)
            .filteredOn(dto -> "Adoption".equals(dto.getAreaOfLawName()))
            .singleElement()
            .satisfies(adoptionArea -> assertThat(adoptionArea.getLocalAuthorities())
                .as("All local authorities should be deselected after clearing")
                .filteredOn(LocalAuthoritySelectionDto::getSelected)
                .isEmpty());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/local-authorities returns 400 when missing required area")
    void shouldReturn400WhenMissingRequiredArea() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Missing Area");

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID childrenId = TestDataHelper.getAreaOfLawIdByName(http, "Children");
        final UUID civilPartnershipId = TestDataHelper.getAreaOfLawIdByName(http, "Civil partnership");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");

        final Response enableAreasResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                        TestDataHelper.buildCourtAreasOfLaw(courtId,
                                                            List.of(adoptionId, childrenId,
                                                                    civilPartnershipId, divorceId)));
        assertThat(enableAreasResponse.statusCode())
            .as("Expected areas of law to be enabled for court %s", courtId)
            .isEqualTo(CREATED.value());

        final List<CourtLocalAuthorityDto> updates = List.of(
            buildCourtLocalAuthorityUpdate(adoptionId, List.of()),
            buildCourtLocalAuthorityUpdate(childrenId, List.of()),
            buildCourtLocalAuthorityUpdate(civilPartnershipId, List.of())
        );

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/local-authorities",
                                                updates);

        assertThat(putResponse.statusCode())
            .as("Expected 400 when missing updates for a required area of law")
            .isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.jsonPath().getString("message"))
            .as("Error message should mention missing area updates")
            .contains("Missing update for area of law");
    }

    /**
     * Builds a court local authority update payload.
     *
     * @param areaOfLawId the area of law ID for the update
     * @param localAuthorities the local authority selections for the area
     * @return the update DTO for the area of law
     */
    private static CourtLocalAuthorityDto buildCourtLocalAuthorityUpdate(
        final UUID areaOfLawId, final List<LocalAuthoritySelectionDto> localAuthorities) {
        return CourtLocalAuthorityDto.builder()
            .areaOfLawId(areaOfLawId)
            .localAuthorities(localAuthorities)
            .build();
    }

    /**
     * Builds a selected local authority payload.
     *
     * @param localAuthorityId the local authority ID
     * @return the selected local authority DTO
     */
    private static LocalAuthoritySelectionDto buildLocalAuthoritySelection(
        final UUID localAuthorityId) {
        return LocalAuthoritySelectionDto.builder()
            .id(localAuthorityId)
            .selected(true)
            .build();
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
