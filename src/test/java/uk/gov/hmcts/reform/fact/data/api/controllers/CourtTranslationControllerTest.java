package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtTranslationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtTranslationControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtTranslationService courtTranslationService;

    @InjectMocks
    private CourtTranslationController courtTranslationController;

    @Test
    void getTranslationServicesReturns200() {
        CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(COURT_ID);
        translation.setEmail("test@example.com");

        when(courtTranslationService.getTranslationByCourtId(COURT_ID)).thenReturn(translation);

        var response = courtTranslationController.getTranslationServicesByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(translation);
    }

    @Test
    void getTranslationServicesThrowsCourtResourceNotFound() {
        when(courtTranslationService.getTranslationByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException("No translation services found"));

        assertThrows(CourtResourceNotFoundException.class, () ->
            courtTranslationController.getTranslationServicesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getTranslationServicesThrowsNotFoundException() {
        when(courtTranslationService.getTranslationByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtTranslationController.getTranslationServicesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getTranslationServicesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtTranslationController.getTranslationServicesByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setTranslationServicesReturns201() {
        CourtTranslation translation = new CourtTranslation();
        translation.setEmail("spanish@example.com");
        translation.setPhoneNumber("1234567890");

        when(courtTranslationService.setTranslation(COURT_ID, translation)).thenReturn(translation);

        var response = courtTranslationController.setTranslationServices(COURT_ID.toString(), translation);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(translation);
    }

    @Test
    void setTranslationServicesAllowsEmptyEmailPhone() {
        CourtTranslation translation = new CourtTranslation();
        translation.setEmail("");
        translation.setPhoneNumber("");

        when(courtTranslationService.setTranslation(COURT_ID, translation)).thenReturn(translation);

        var response = courtTranslationController.setTranslationServices(COURT_ID.toString(), translation);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(translation);
    }

    @Test
    void setTranslationServicesThrowsNotFoundExceptionWhenCourtNotFound() {
        CourtTranslation translation = new CourtTranslation();
        translation.setEmail("test@example.com");

        when(courtTranslationService.setTranslation(UNKNOWN_COURT_ID, translation))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtTranslationController.setTranslationServices(UNKNOWN_COURT_ID.toString(), translation)
        );
    }

    @Test
    void setTranslationServicesThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtTranslation translation = new CourtTranslation();
        translation.setEmail("test@example.com");

        assertThrows(IllegalArgumentException.class, () ->
            courtTranslationController.setTranslationServices(INVALID_UUID, translation)
        );
    }
}
