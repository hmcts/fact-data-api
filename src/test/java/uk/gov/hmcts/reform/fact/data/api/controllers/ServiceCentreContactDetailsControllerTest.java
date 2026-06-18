package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreContactDetailsService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreContactDetailsControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();

    @Mock
    private ServiceCentreContactDetailsService serviceCentreContactDetailsService;

    @InjectMocks
    private ServiceCentreContactDetailsController serviceCentreContactDetailsController;

    @Test
    void getContactDetailsReturns200() {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder().id(CONTACT_ID).build();
        when(serviceCentreContactDetailsService.getContactDetails(SERVICE_CENTRE_ID))
            .thenReturn(List.of(contactDetails));

        ResponseEntity<List<ServiceCentreContactDetails>> response =
            serviceCentreContactDetailsController.getContactDetails(SERVICE_CENTRE_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(contactDetails);
    }

    @Test
    void getContactDetailReturns200() {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder().id(CONTACT_ID).build();
        when(serviceCentreContactDetailsService.getContactDetail(SERVICE_CENTRE_ID, CONTACT_ID))
            .thenReturn(contactDetails);

        ResponseEntity<ServiceCentreContactDetails> response =
            serviceCentreContactDetailsController.getContactDetail(SERVICE_CENTRE_ID.toString(), CONTACT_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(contactDetails);
    }

    @Test
    void createContactDetailReturns201() {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder().id(CONTACT_ID).build();
        when(serviceCentreContactDetailsService.createContactDetail(SERVICE_CENTRE_ID, contactDetails))
            .thenReturn(contactDetails);

        ResponseEntity<ServiceCentreContactDetails> response =
            serviceCentreContactDetailsController.createContactDetail(SERVICE_CENTRE_ID.toString(), contactDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(contactDetails);
    }

    @Test
    void updateContactDetailReturns200() {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder().id(CONTACT_ID).build();
        when(serviceCentreContactDetailsService.updateContactDetail(SERVICE_CENTRE_ID, CONTACT_ID, contactDetails))
            .thenReturn(contactDetails);

        ResponseEntity<ServiceCentreContactDetails> response =
            serviceCentreContactDetailsController.updateContactDetail(
                SERVICE_CENTRE_ID.toString(),
                CONTACT_ID.toString(),
                contactDetails
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(contactDetails);
    }

    @Test
    void deleteContactDetailReturns204() {
        ResponseEntity<Void> response = serviceCentreContactDetailsController.deleteContactDetail(
            SERVICE_CENTRE_ID.toString(),
            CONTACT_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(serviceCentreContactDetailsService).deleteContactDetail(SERVICE_CENTRE_ID, CONTACT_ID);
    }
}
