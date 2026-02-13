package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.dto.ProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Professional Information Controller")
@DisplayName("Court Professional Information Controller")
public final class CourtProfessionalInformationControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information with all valid fields")
    void shouldCreateProfessionalInformationWithAllValidFields() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Full Professional Info");

        final ZonedDateTime timestampBeforeCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        final ProfessionalInformationDto professionalInfo = new ProfessionalInformationDto();
        professionalInfo.setInterviewRooms(true);
        professionalInfo.setInterviewRoomCount(6);
        professionalInfo.setInterviewPhoneNumber("0207 123 4567");
        professionalInfo.setVideoHearings(true);
        professionalInfo.setCommonPlatform(true);
        professionalInfo.setAccessScheme(true);

        final CourtCodesDto codes = new CourtCodesDto();
        codes.setMagistrateCourtCode(123456);
        codes.setFamilyCourtCode(234567);
        codes.setTribunalCode(345678);
        codes.setCountyCourtCode(456789);
        codes.setCrownCourtCode(567890);
        codes.setGbs("GBS001");

        final CourtDxCodeDto dxCode1 = new CourtDxCodeDto();
        dxCode1.setDxCode("120551 Marylebone 9");
        dxCode1.setExplanation("(County Court) primary");

        final CourtDxCodeDto dxCode2 = new CourtDxCodeDto();
        dxCode2.setDxCode("703360 Hanley 3");
        dxCode2.setExplanation("(Crown Court) secondary");

        final CourtFaxDto fax1 = new CourtFaxDto();
        fax1.setFaxNumber("0207 222 3333");
        fax1.setDescription("General enquiries");

        final CourtFaxDto fax2 = new CourtFaxDto();
        fax2.setFaxNumber("0207 444 5555");
        fax2.setDescription("Urgent matters");

        final CourtProfessionalInformationDetailsDto professionalInfoDetails =
            new CourtProfessionalInformationDetailsDto();
        professionalInfoDetails.setProfessionalInformation(professionalInfo);
        professionalInfoDetails.setCodes(codes);
        professionalInfoDetails.setDxCodes(List.of(dxCode1, dxCode2));
        professionalInfoDetails.setFaxNumbers(List.of(fax1, fax2));

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/professional-information",
                                                   professionalInfoDetails);
        assertThat(postResponse.statusCode())
            .as("Expected 201 CREATED when creating professional info for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtProfessionalInformationDetailsDto createdInfo = mapper.readValue(
            postResponse.asString(),
            CourtProfessionalInformationDetailsDto.class
        );

        assertThat(createdInfo.getProfessionalInformation())
            .as("Created professional information should be valid")
            .isNotNull()
            .satisfies(info -> {
                assertThat(info.getInterviewRooms()).as("Interview rooms").isTrue();
                assertThat(info.getInterviewRoomCount()).as("Interview room count").isEqualTo(6);
                assertThat(info.getInterviewPhoneNumber()).as("Interview phone").isEqualTo("0207 123 4567");
                assertThat(info.getVideoHearings()).as("Video hearings").isTrue();
            });

        assertThat(createdInfo.getCodes())
            .as("Created codes should be valid")
            .isNotNull()
            .satisfies(c -> assertThat(c.getGbs()).as("GBS code").isEqualTo("GBS001"));

        assertThat(createdInfo.getDxCodes()).as("DX codes count").hasSize(2);
        assertThat(createdInfo.getFaxNumbers()).as("Fax numbers count").hasSize(2);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/professional-information");
        assertThat(getResponse.statusCode())
            .as("Expected 200 OK when retrieving professional info for court %s", courtId)
            .isEqualTo(OK.value());

        final CourtProfessionalInformationDetailsDto retrievedInfo = mapper.readValue(
            getResponse.asString(),
            CourtProfessionalInformationDetailsDto.class
        );

        assertThat(retrievedInfo.getProfessionalInformation())
            .as("Retrieved professional information should be valid")
            .isNotNull()
            .satisfies(info -> {
                assertThat(info.getInterviewRooms()).as("Interview rooms").isTrue();
                assertThat(info.getInterviewRoomCount()).as("Interview room count").isEqualTo(6);
                assertThat(info.getInterviewPhoneNumber()).as("Interview phone").isEqualTo("0207 123 4567");
                assertThat(info.getVideoHearings()).as("Video hearings").isTrue();
                assertThat(info.getCommonPlatform()).as("Common platform").isTrue();
                assertThat(info.getAccessScheme()).as("Access scheme").isTrue();
            });

        assertThat(retrievedInfo.getCodes())
            .as("Retrieved codes should be valid")
            .isNotNull()
            .satisfies(c -> {
                assertThat(c.getMagistrateCourtCode()).as("Magistrate court code").isEqualTo(123456);
                assertThat(c.getGbs()).as("GBS code").isEqualTo("GBS001");
            });

        assertThat(retrievedInfo.getDxCodes())
            .as("Retrieved DX codes should be valid")
            .hasSize(2)
            .first()
            .satisfies(dx -> assertThat(dx.getDxCode()).as("First DX code").isEqualTo("120551 Marylebone 9"));

        assertThat(retrievedInfo.getFaxNumbers())
            .as("Retrieved fax numbers should be valid")
            .hasSize(2)
            .first()
            .satisfies(fax -> assertThat(fax.getFaxNumber()).as("First fax number").isEqualTo("0207 222 3333"));

        final ZonedDateTime timestampAfterCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterCreate)
            .as("Court lastUpdatedAt should move forward after professional info creation for court %s", courtId)
            .isAfter(timestampBeforeCreate);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information updates existing professional information")
    void shouldUpdateExistingProfessionalInformation() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Update Professional Info");

        final ProfessionalInformationDto initialInfo = new ProfessionalInformationDto();
        initialInfo.setInterviewRooms(true);
        initialInfo.setInterviewRoomCount(3);
        initialInfo.setInterviewPhoneNumber("0207 111 2222");
        initialInfo.setVideoHearings(false);
        initialInfo.setCommonPlatform(false);
        initialInfo.setAccessScheme(false);

        final CourtProfessionalInformationDetailsDto initialDetails =
            new CourtProfessionalInformationDetailsDto();
        initialDetails.setProfessionalInformation(initialInfo);

        final Response createResponse = http.doPost("/courts/" + courtId + "/v1/professional-information",
                                                     initialDetails);
        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating initial professional info for court %s", courtId)
            .isEqualTo(CREATED.value());

        final ProfessionalInformationDto updatedInfo = new ProfessionalInformationDto();
        updatedInfo.setInterviewRooms(true);
        updatedInfo.setInterviewRoomCount(10);
        updatedInfo.setInterviewPhoneNumber("0207 999 8888");
        updatedInfo.setVideoHearings(true);
        updatedInfo.setCommonPlatform(true);
        updatedInfo.setAccessScheme(true);

        final CourtCodesDto updatedCodes = new CourtCodesDto();
        updatedCodes.setMagistrateCourtCode(999999);
        updatedCodes.setGbs("GBS999");

        final CourtDxCodeDto updatedDxCode = new CourtDxCodeDto();
        updatedDxCode.setDxCode("999999 Updated 1");
        updatedDxCode.setExplanation("Updated explanation");

        final CourtProfessionalInformationDetailsDto updatedDetails =
            new CourtProfessionalInformationDetailsDto();
        updatedDetails.setProfessionalInformation(updatedInfo);
        updatedDetails.setCodes(updatedCodes);
        updatedDetails.setDxCodes(List.of(updatedDxCode));

        final Response updateResponse = http.doPost("/courts/" + courtId + "/v1/professional-information",
                                                     updatedDetails);
        assertThat(updateResponse.statusCode())
            .as("Expected 201 CREATED when updating professional info for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/professional-information");
        assertThat(getResponse.statusCode())
            .as("Expected 200 OK when retrieving updated professional info for court %s", courtId)
            .isEqualTo(OK.value());

        final CourtProfessionalInformationDetailsDto retrievedInfo = mapper.readValue(
            getResponse.asString(),
            CourtProfessionalInformationDetailsDto.class
        );

        assertThat(retrievedInfo.getProfessionalInformation())
            .as("Updated professional information should reflect changes")
            .satisfies(info -> {
                assertThat(info.getInterviewRoomCount()).as("Interview room count").isEqualTo(10);
                assertThat(info.getInterviewPhoneNumber()).as("Interview phone").isEqualTo("0207 999 8888");
                assertThat(info.getVideoHearings()).as("Video hearings").isTrue();
                assertThat(info.getCommonPlatform()).as("Common platform").isTrue();
            });

        assertThat(retrievedInfo.getCodes())
            .as("Updated codes should reflect changes")
            .isNotNull()
            .satisfies(codes -> {
                assertThat(codes.getMagistrateCourtCode()).as("Magistrate court code").isEqualTo(999999);
                assertThat(codes.getGbs()).as("GBS code").isEqualTo("GBS999");
            });

        assertThat(retrievedInfo.getDxCodes())
            .as("Updated DX codes should reflect changes")
            .hasSize(1)
            .first()
            .satisfies(dx -> assertThat(dx.getDxCode()).as("DX code").isEqualTo("999999 Updated 1"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/professional-information returns 204 when no professional info exists")
    void shouldReturn204WhenNoProfessionalInformation() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Professional Info");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/professional-information");
        assertThat(getResponse.statusCode())
            .as("Expected 204 NO_CONTENT when no professional info exists for court %s", courtId)
            .isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/professional-information fails with non-existent court")
    void shouldFailToGetProfessionalInformationForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId
                                                    + "/v1/professional-information");
        assertThat(getResponse.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .as("Error message should indicate court not found")
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information fails with non-existent court")
    void shouldFailToCreateProfessionalInformationForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final ProfessionalInformationDto professionalInfo = new ProfessionalInformationDto();
        professionalInfo.setInterviewRooms(true);
        professionalInfo.setInterviewRoomCount(5);
        professionalInfo.setInterviewPhoneNumber("0207 123 4567");
        professionalInfo.setVideoHearings(true);
        professionalInfo.setCommonPlatform(true);
        professionalInfo.setAccessScheme(true);

        final CourtProfessionalInformationDetailsDto professionalInfoDetails =
            new CourtProfessionalInformationDetailsDto();
        professionalInfoDetails.setProfessionalInformation(professionalInfo);

        final Response postResponse = http.doPost("/courts/" + nonExistentCourtId
                                                      + "/v1/professional-information",
                                                   professionalInfoDetails);
        assertThat(postResponse.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .as("Error message should indicate court not found")
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
