package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.TranslationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServicesControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private TranslationServicesController translationServicesController;

    @Test
    void getTranslationServicesReturns200() {
        Translation translation = new Translation();
        translation.setCourtId(COURT_ID);
        translation.setEmail("test@example.com");

        when(translationService.getTranslationByCourtId(COURT_ID)).thenReturn(translation);

        var response = translationServicesController.getTranslationServicesByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(translation);
    }

    @Test
    void getTranslationServicesThrowsTranslationNotFound() {
        when(translationService.getTranslationByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new TranslationNotFoundException("No translation services found"));

        assertThrows(TranslationNotFoundException.class, () ->
            translationServicesController.getTranslationServicesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getTranslationServicesThrowsNotFoundException() {
        when(translationService.getTranslationByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            translationServicesController.getTranslationServicesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getTranslationServicesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            translationServicesController.getTranslationServicesByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setTranslationServicesReturns201() {
        Translation translation = new Translation();
        translation.setEmail("spanish@example.com");
        translation.setPhoneNumber("1234567890");

        when(translationService.setTranslation(COURT_ID, translation)).thenReturn(translation);

        var response = translationServicesController.setTranslationServices(COURT_ID.toString(), translation);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(translation);
    }

    @Test
    void setTranslationServicesAllowsEmptyEmailPhone() {
        Translation translation = new Translation();
        translation.setEmail("");
        translation.setPhoneNumber("");

        when(translationService.setTranslation(COURT_ID, translation)).thenReturn(translation);

        var response = translationServicesController.setTranslationServices(COURT_ID.toString(), translation);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(translation);
    }

    @Test
    void setTranslationServicesThrowsNotFoundExceptionWhenCourtNotFound() {
        Translation translation = new Translation();
        translation.setEmail("test@example.com");

        when(translationService.setTranslation(UNKNOWN_COURT_ID, translation))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            translationServicesController.setTranslationServices(UNKNOWN_COURT_ID.toString(), translation)
        );
    }

    @Test
    void setTranslationServicesThrowsIllegalArgumentExceptionForInvalidUUID() {
        Translation translation = new Translation();
        translation.setEmail("test@example.com");

        assertThrows(IllegalArgumentException.class, () ->
            translationServicesController.setTranslationServices(INVALID_UUID, translation)
        );
    }
}
