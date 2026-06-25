package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAddressService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreAddressControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    @Mock
    private ServiceCentreAddressService serviceCentreAddressService;

    @InjectMocks
    private ServiceCentreAddressController serviceCentreAddressController;

    @Test
    void getAddressesReturns200() {
        ServiceCentreAddress address = ServiceCentreAddress.builder().id(ADDRESS_ID).build();
        when(serviceCentreAddressService.getAddresses(SERVICE_CENTRE_ID)).thenReturn(List.of(address));

        ResponseEntity<List<ServiceCentreAddress>> response =
            serviceCentreAddressController.getAddresses(SERVICE_CENTRE_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(address);
    }

    @Test
    void getAddressReturns200() {
        ServiceCentreAddress address = ServiceCentreAddress.builder().id(ADDRESS_ID).build();
        when(serviceCentreAddressService.getAddress(SERVICE_CENTRE_ID, ADDRESS_ID)).thenReturn(address);

        ResponseEntity<ServiceCentreAddress> response =
            serviceCentreAddressController.getAddress(SERVICE_CENTRE_ID.toString(), ADDRESS_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void createAddressReturns201() {
        ServiceCentreAddress address = ServiceCentreAddress.builder().id(ADDRESS_ID).build();
        when(serviceCentreAddressService.createAddress(SERVICE_CENTRE_ID, address)).thenReturn(address);

        ResponseEntity<ServiceCentreAddress> response =
            serviceCentreAddressController.createAddress(SERVICE_CENTRE_ID.toString(), address);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void updateAddressReturns200() {
        ServiceCentreAddress address = ServiceCentreAddress.builder().id(ADDRESS_ID).build();
        when(serviceCentreAddressService.updateAddress(SERVICE_CENTRE_ID, ADDRESS_ID, address)).thenReturn(address);

        ResponseEntity<ServiceCentreAddress> response = serviceCentreAddressController.updateServiceCentreAddress(
            SERVICE_CENTRE_ID.toString(),
            ADDRESS_ID.toString(),
            address
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void deleteAddressReturns204() {
        ResponseEntity<Void> response = serviceCentreAddressController.deleteServiceCentreAddress(
            SERVICE_CENTRE_ID.toString(),
            ADDRESS_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(serviceCentreAddressService).deleteAddress(SERVICE_CENTRE_ID, ADDRESS_ID);
    }
}
