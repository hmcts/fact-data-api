package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.dto.ProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtProfessionalInformationService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtProfessionalInformationController.class)
class CourtProfessionalInformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtProfessionalInformationService courtProfessionalInformationService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private CourtProfessionalInformationDetailsDto sampleProfessionalInformationDetails() {
        ProfessionalInformationDto professionalInformation = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(3)
            .interviewPhoneNumber("01234567890")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        CourtCodesDto codes = CourtCodesDto.builder()
            .magistrateCourtCode(111)
            .familyCourtCode(222)
            .gbs("333")
            .build();

        CourtDxCodeDto dxCode = CourtDxCodeDto.builder()
            .dxCode("444")
            .explanation("Main DX")
            .build();

        CourtFaxDto fax = CourtFaxDto.builder()
            .faxNumber("01234567890")
            .description("Primary fax")
            .build();

        return CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(professionalInformation)
            .codes(codes)
            .dxCodes(List.of(dxCode))
            .faxNumbers(List.of(fax))
            .build();
    }

    private CourtProfessionalInformationDetailsDto wrapProfessionalInformation(
        ProfessionalInformationDto professionalInformation
    ) {
        CourtCodesDto codes = CourtCodesDto.builder()
            .magistrateCourtCode(111)
            .gbs("333")
            .build();

        CourtDxCodeDto dxCode = CourtDxCodeDto.builder()
            .dxCode("444")
            .explanation("Main DX")
            .build();

        CourtFaxDto fax = CourtFaxDto.builder()
            .faxNumber("01234567890")
            .description("Primary fax")
            .build();

        return buildDetails(professionalInformation, codes, List.of(dxCode), List.of(fax));
    }

    private CourtProfessionalInformationDetailsDto buildDetails(
        ProfessionalInformationDto professionalInformation,
        CourtCodesDto codes,
        List<CourtDxCodeDto> dxCodes,
        List<CourtFaxDto> faxNumbers
    ) {
        return CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(professionalInformation)
            .codes(codes)
            .dxCodes(dxCodes)
            .faxNumbers(faxNumbers)
            .build();
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/professional-information returns professional information successfully")
    void getProfessionalInformationReturnsSuccessfully() throws Exception {
        CourtProfessionalInformationDetailsDto professionalInformation = sampleProfessionalInformationDetails();

        when(courtProfessionalInformationService.getProfessionalInformationByCourtId(courtId))
            .thenReturn(professionalInformation);

        mockMvc.perform(get("/courts/{courtId}/v1/professional-information", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.professionalInformation.interviewRoomCount").value(3))
            .andExpect(jsonPath("$.professionalInformation.videoHearings").value(true))
            .andExpect(jsonPath("$.codes.gbs").value("333"))
            .andExpect(jsonPath("$.dxCodes[0].dxCode").value("444"))
            .andExpect(jsonPath("$.faxNumbers[0].faxNumber").value("01234567890"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/professional-information returns 404 if court does not exist")
    void getProfessionalInformationNonExistentCourtReturnsNotFound() throws Exception {
        when(courtProfessionalInformationService.getProfessionalInformationByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/professional-information", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/professional-information returns 204 if information does not exist")
    void getProfessionalInformationReturnsNoContentWhenAbsent() throws Exception {
        when(courtProfessionalInformationService.getProfessionalInformationByCourtId(courtId))
            .thenThrow(new CourtResourceNotFoundException("Not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/professional-information", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/professional-information returns 400 for invalid UUID")
    void getProfessionalInformationInvalidUuid() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/professional-information", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information creates information successfully")
    void postProfessionalInformationCreatesSuccessfully() throws Exception {
        CourtProfessionalInformationDetailsDto professionalInformation = sampleProfessionalInformationDetails();

        when(courtProfessionalInformationService.setProfessionalInformation(
            any(UUID.class),
            any(CourtProfessionalInformationDetailsDto.class)
        )).thenReturn(professionalInformation);

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(professionalInformation)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.professionalInformation.interviewRoomCount").value(3))
            .andExpect(jsonPath("$.professionalInformation.accessScheme").value(true))
            .andExpect(jsonPath("$.codes.magistrateCourtCode").value(111))
            .andExpect(jsonPath("$.dxCodes[0].explanation").value("Main DX"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 404 if court does not exist")
    void postProfessionalInformationNonExistentCourtReturnsNotFound() throws Exception {
        CourtProfessionalInformationDetailsDto professionalInformation = sampleProfessionalInformationDetails();

        when(courtProfessionalInformationService.setProfessionalInformation(
            any(UUID.class),
            any(CourtProfessionalInformationDetailsDto.class)
        )).thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(professionalInformation)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid phone number")
    void postProfessionalInformationInvalidPhoneReturnsBadRequest() throws Exception {
        ProfessionalInformationDto invalidProfessionalInformation = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(3)
            .interviewPhoneNumber("123")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                wrapProfessionalInformation(invalidProfessionalInformation)
                            )))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid interview room count")
    void postProfessionalInformationInvalidInterviewRoomCountReturnsBadRequest() throws Exception {
        ProfessionalInformationDto invalidProfessionalInformation = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(0)
            .interviewPhoneNumber("01234567890")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                wrapProfessionalInformation(invalidProfessionalInformation)
                            )))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 when interview room count exceeds 150")
    void postProfessionalInformationInterviewRoomCountTooHighReturnsBadRequest() throws Exception {
        ProfessionalInformationDto invalidProfessionalInformation = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(200)
            .interviewPhoneNumber("01234567890")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                wrapProfessionalInformation(invalidProfessionalInformation)
                            )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['professionalInformation.interviewRoomCountConsistent']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 if rooms false and count set")
    void postProfessionalInformationInvalidInterviewRoomsCombinationReturnsBadRequest() throws Exception {
        ProfessionalInformationDto invalidProfessionalInformation = ProfessionalInformationDto.builder()
            .interviewRooms(false)
            .interviewRoomCount(2)
            .interviewPhoneNumber("01234567890")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                wrapProfessionalInformation(invalidProfessionalInformation)
                            )))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid DX code characters")
    void postProfessionalInformationInvalidDxCodeCharactersReturnsBadRequest() throws Exception {
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode("120551 Marylebone 9$")
                .explanation("Valid explanation")
                .build()),
            List.of(CourtFaxDto.builder()
                .faxNumber("0207 111 1111")
                .description("Fax")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['dxCodes[0].dxCode']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for DX code too long")
    void postProfessionalInformationInvalidDxCodeTooLongReturnsBadRequest() throws Exception {
        String longCode = ("DX" + "0123456789".repeat(21));
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode(longCode)
                .explanation("Valid explanation")
                .build()),
            List.of(CourtFaxDto.builder()
                .faxNumber("0207 111 1111")
                .description("Fax")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['dxCodes[0].dxCode']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for DX explanation too long")
    void postProfessionalInformationInvalidDxExplanationTooLongReturnsBadRequest() throws Exception {
        String longExplanation = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(6);
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode("120551 Marylebone 9")
                .explanation(longExplanation)
                .build()),
            List.of(CourtFaxDto.builder()
                .faxNumber("0207 111 1111")
                .description("Fax")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['dxCodes[0].explanation']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid GBS code")
    void postProfessionalInformationInvalidGbsReturnsBadRequest() throws Exception {
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder()
                .magistrateCourtCode(111111)
                .gbs("INVALID#")
                .build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode("120551 Marylebone 9")
                .explanation("Valid explanation")
                .build()),
            List.of(CourtFaxDto.builder()
                .faxNumber("0207 111 1111")
                .description("Fax")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['codes.gbs']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid fax description")
    void postProfessionalInformationInvalidFaxDescriptionReturnsBadRequest() throws Exception {
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode("120551 Marylebone 9")
                .explanation("Valid explanation")
                .build()),
            List.of(CourtFaxDto.builder()
                .faxNumber("0207 111 1111")
                .description("Invalid 😊")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['faxNumbers[0].description']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid fax number")
    void postProfessionalInformationInvalidFaxNumberReturnsBadRequest() throws Exception {
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode("120551 Marylebone 9")
                .explanation("Valid explanation")
                .build()),
            List.of(CourtFaxDto.builder()
                .faxNumber("012")
                .description("Fax")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['faxNumbers[0].faxNumber']").exists());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 when DX array contains null element")
    void postProfessionalInformationNullDxEntryReturnsBadRequest() throws Exception {
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            Arrays.asList((CourtDxCodeDto) null),
            List.of(CourtFaxDto.builder()
                .faxNumber("0207 111 1111")
                .description("Fax")
                .build())
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['dxCodes[0]']").value("must not be null"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 when fax array contains null element")
    void postProfessionalInformationNullFaxEntryReturnsBadRequest() throws Exception {
        CourtProfessionalInformationDetailsDto invalid = buildDetails(
            ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(2)
                .interviewPhoneNumber("0207 123 4567")
                .videoHearings(true)
                .commonPlatform(false)
                .accessScheme(true)
                .build(),
            CourtCodesDto.builder().gbs("123").build(),
            List.of(CourtDxCodeDto.builder()
                .dxCode("120551 Marylebone 9")
                .explanation("Valid explanation")
                .build()),
            Arrays.asList((CourtFaxDto) null)
        );

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$['faxNumbers[0]']").value("must not be null"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/professional-information returns 400 for invalid UUID")
    void postProfessionalInformationInvalidUuid() throws Exception {
        CourtProfessionalInformationDetailsDto professionalInformation = sampleProfessionalInformationDetails();

        mockMvc.perform(post("/courts/{courtId}/v1/professional-information", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(professionalInformation)))
            .andExpect(status().isBadRequest());
    }
}
