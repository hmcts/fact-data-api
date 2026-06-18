package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();

    @Mock
    private ServiceCentreService serviceCentreService;

    @InjectMocks
    private ServiceCentreController serviceCentreController;

    @Test
    void getServiceCentreByIdReturns200() {
        ServiceCentre serviceCentre = createServiceCentre();
        when(serviceCentreService.getServiceCentreById(SERVICE_CENTRE_ID)).thenReturn(serviceCentre);

        ResponseEntity<ServiceCentre> response =
            serviceCentreController.getServiceCentreById(SERVICE_CENTRE_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceCentre);
    }

    @Test
    void getServiceCentreByNameReturns200() {
        ServiceCentre serviceCentre = createServiceCentre();
        when(serviceCentreService.getServiceCentreByName("Test Service Centre")).thenReturn(serviceCentre);

        ResponseEntity<ServiceCentre> response =
            serviceCentreController.getServiceCentreByName("Test Service Centre");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceCentre);
    }

    @Test
    void createServiceCentreReturns201() {
        ServiceCentre serviceCentre = createServiceCentre();
        when(serviceCentreService.createServiceCentre(serviceCentre)).thenReturn(serviceCentre);

        ResponseEntity<ServiceCentre> response = serviceCentreController.createServiceCentre(serviceCentre);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(serviceCentre);
    }

    @Test
    void updateServiceCentreReturns200() {
        ServiceCentre serviceCentre = createServiceCentre();
        when(serviceCentreService.updateServiceCentre(SERVICE_CENTRE_ID, serviceCentre)).thenReturn(serviceCentre);

        ResponseEntity<ServiceCentre> response =
            serviceCentreController.updateServiceCentre(SERVICE_CENTRE_ID.toString(), serviceCentre);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceCentre);
    }

    private ServiceCentre createServiceCentre() {
        return ServiceCentre.builder()
            .id(SERVICE_CENTRE_ID)
            .name("Test Service Centre")
            .slug("test-service-centre")
            .open(true)
            .build();
    }
}
