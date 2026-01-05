package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.dto.ProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtProfessionalInformationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtProfessionalInformationControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtProfessionalInformationService courtProfessionalInformationService;

    @InjectMocks
    private CourtProfessionalInformationController courtProfessionalInformationController;

    private CourtProfessionalInformationDetailsDto buildProfessionalInformationDetails() {
        ProfessionalInformationDto professionalInformation = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(2)
            .interviewPhoneNumber("01234567890")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        CourtCodesDto codes = CourtCodesDto.builder()
            .magistrateCourtCode(123)
            .gbs("456")
            .build();

        CourtDxCodeDto dxCode = CourtDxCodeDto.builder()
            .dxCode("789")
            .explanation("DX explanation")
            .build();

        CourtFaxDto fax = CourtFaxDto.builder()
            .faxNumber("01234567890")
            .description("Fax description")
            .build();

        return CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(professionalInformation)
            .codes(codes)
            .dxCodes(List.of(dxCode))
            .faxNumbers(List.of(fax))
            .build();
    }

    @Test
    void getProfessionalInformationReturns200() {
        CourtProfessionalInformationDetailsDto professionalInformation = buildProfessionalInformationDetails();

        when(courtProfessionalInformationService.getProfessionalInformationByCourtId(COURT_ID))
            .thenReturn(professionalInformation);

        ResponseEntity<CourtProfessionalInformationDetailsDto> response =
            courtProfessionalInformationController.getProfessionalInformationByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(professionalInformation);
    }

    @Test
    void getProfessionalInformationThrowsCourtResourceNotFound() {
        when(courtProfessionalInformationService.getProfessionalInformationByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException("Not found"));

        assertThrows(CourtResourceNotFoundException.class, () ->
            courtProfessionalInformationController.getProfessionalInformationByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getProfessionalInformationThrowsNotFound() {
        when(courtProfessionalInformationService.getProfessionalInformationByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtProfessionalInformationController.getProfessionalInformationByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getProfessionalInformationThrowsIllegalArgumentForInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () ->
            courtProfessionalInformationController.getProfessionalInformationByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setProfessionalInformationReturns201() {
        CourtProfessionalInformationDetailsDto professionalInformation = buildProfessionalInformationDetails();

        when(courtProfessionalInformationService.setProfessionalInformation(COURT_ID, professionalInformation))
            .thenReturn(professionalInformation);

        ResponseEntity<CourtProfessionalInformationDetailsDto> response =
            courtProfessionalInformationController.setProfessionalInformation(
                COURT_ID.toString(),
                professionalInformation
            );

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(professionalInformation);
    }

    @Test
    void setProfessionalInformationThrowsNotFound() {
        CourtProfessionalInformationDetailsDto professionalInformation = buildProfessionalInformationDetails();

        when(courtProfessionalInformationService.setProfessionalInformation(
            UNKNOWN_COURT_ID,
            professionalInformation
        )).thenThrow(new NotFoundException("Court not found"));

        assertThrows(
            NotFoundException.class,
            () -> courtProfessionalInformationController.setProfessionalInformation(
                UNKNOWN_COURT_ID.toString(),
                professionalInformation
            )
        );
    }

    @Test
    void setProfessionalInformationThrowsIllegalArgumentForInvalidUuid() {
        CourtProfessionalInformationDetailsDto professionalInformation = buildProfessionalInformationDetails();

        assertThrows(
            IllegalArgumentException.class,
            () -> courtProfessionalInformationController.setProfessionalInformation(
                INVALID_UUID,
                professionalInformation
            )
        );
    }
}
