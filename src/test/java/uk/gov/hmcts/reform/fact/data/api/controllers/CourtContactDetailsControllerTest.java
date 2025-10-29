package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.services.CourtContactDetailsService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtContactDetailsControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid";

    @Mock
    private CourtContactDetailsService courtContactDetailsService;

    @InjectMocks
    private CourtContactDetailsController courtContactDetailsController;

    @Test
    void getContactDetailsReturns200() {
        CourtContactDetails contactDetail = new CourtContactDetails();
        when(courtContactDetailsService.getContactDetails(COURT_ID)).thenReturn(List.of(contactDetail));

        ResponseEntity<List<CourtContactDetails>> response =
            courtContactDetailsController.getContactDetails(COURT_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(contactDetail);
    }

    @Test
    void getContactDetailsThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtContactDetailsController.getContactDetails(INVALID_UUID)
        );
    }

    @Test
    void getContactDetailReturns200() {
        CourtContactDetails contactDetail = new CourtContactDetails();
        when(courtContactDetailsService.getContactDetail(COURT_ID, CONTACT_ID)).thenReturn(contactDetail);

        ResponseEntity<CourtContactDetails> response = courtContactDetailsController.getContactDetail(
            COURT_ID.toString(),
            CONTACT_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(contactDetail);
    }

    @Test
    void getContactDetailThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtContactDetailsController.getContactDetail(INVALID_UUID, CONTACT_ID.toString())
        );
    }

    @Test
    void createContactDetailReturns201() {
        CourtContactDetails contactDetail = new CourtContactDetails();
        when(courtContactDetailsService.createContactDetail(COURT_ID, contactDetail)).thenReturn(contactDetail);

        ResponseEntity<CourtContactDetails> response =
            courtContactDetailsController.createContactDetail(COURT_ID.toString(), contactDetail);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(contactDetail);
    }

    @Test
    void createContactDetailThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtContactDetails contactDetail = new CourtContactDetails();

        assertThrows(IllegalArgumentException.class, () ->
            courtContactDetailsController.createContactDetail(INVALID_UUID, contactDetail)
        );
    }

    @Test
    void updateContactDetailReturns200() {
        CourtContactDetails contactDetail = new CourtContactDetails();
        when(courtContactDetailsService.updateContactDetail(COURT_ID, CONTACT_ID, contactDetail))
            .thenReturn(contactDetail);

        ResponseEntity<CourtContactDetails> response = courtContactDetailsController.updateContactDetail(
            COURT_ID.toString(),
            CONTACT_ID.toString(),
            contactDetail
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(contactDetail);
    }

    @Test
    void updateContactDetailThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtContactDetails contactDetail = new CourtContactDetails();

        assertThrows(IllegalArgumentException.class, () ->
            courtContactDetailsController.updateContactDetail(INVALID_UUID, CONTACT_ID.toString(), contactDetail)
        );
    }

    @Test
    void deleteContactDetailReturns204() {
        ResponseEntity<Void> response = courtContactDetailsController.deleteContactDetail(
            COURT_ID.toString(),
            CONTACT_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courtContactDetailsService).deleteContactDetail(COURT_ID, CONTACT_ID);
    }

    @Test
    void deleteContactDetailThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtContactDetailsController.deleteContactDetail(INVALID_UUID, CONTACT_ID.toString())
        );
    }
}
