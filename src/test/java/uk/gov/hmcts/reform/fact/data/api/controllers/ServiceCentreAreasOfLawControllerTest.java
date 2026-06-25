package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAreasOfLawService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreAreasOfLawControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();

    @Mock
    private ServiceCentreAreasOfLawService serviceCentreAreasOfLawService;

    @InjectMocks
    private ServiceCentreAreasOfLawController serviceCentreAreasOfLawController;

    @Test
    void getAreasOfLawByServiceCentreIdReturns200() {
        AreaOfLawType areaOfLawType = new AreaOfLawType();
        Map<AreaOfLawType, Boolean> responseBody = Map.of(areaOfLawType, true);

        when(serviceCentreAreasOfLawService.getAreasOfLawStatusByServiceCentreId(SERVICE_CENTRE_ID))
            .thenReturn(responseBody);

        ResponseEntity<Map<AreaOfLawType, Boolean>> response =
            serviceCentreAreasOfLawController.getAreasOfLawByServiceCentreId(SERVICE_CENTRE_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    @Test
    void setAreasOfLawServicesReturns201() {
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder().build();

        when(serviceCentreAreasOfLawService.setServiceCentreAreasOfLaw(SERVICE_CENTRE_ID, areasOfLaw))
            .thenReturn(areasOfLaw);

        ResponseEntity<ServiceCentreAreasOfLaw> response =
            serviceCentreAreasOfLawController.setAreasOfLawServices(SERVICE_CENTRE_ID.toString(), areasOfLaw);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(areasOfLaw);
    }
}
