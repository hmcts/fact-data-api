package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAddressService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAddressControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid";

    @Mock
    private CourtAddressService courtAddressService;

    @InjectMocks
    private CourtAddressController courtAddressController;

    @Test
    void getAddressesReturns200() {
        CourtAddress address = new CourtAddress();
        when(courtAddressService.getAddresses(COURT_ID)).thenReturn(List.of(address));

        ResponseEntity<List<CourtAddress>> response =
            courtAddressController.getAddresses(COURT_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(address);
    }

    @Test
    void getAddressesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressController.getAddresses(INVALID_UUID)
        );
    }

    @Test
    void getAddressReturns200() {
        CourtAddress address = new CourtAddress();
        when(courtAddressService.getAddress(COURT_ID, ADDRESS_ID)).thenReturn(address);

        ResponseEntity<CourtAddress> response = courtAddressController.getAddress(
            COURT_ID.toString(),
            ADDRESS_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void getAddressThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressController.getAddress(INVALID_UUID, ADDRESS_ID.toString())
        );
    }

    @Test
    void createAddressReturns201() {
        CourtAddress address = new CourtAddress();
        when(courtAddressService.createAddress(COURT_ID, address)).thenReturn(address);

        ResponseEntity<CourtAddress> response =
            courtAddressController.createAddress(COURT_ID.toString(), address);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void createAddressThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtAddress address = new CourtAddress();

        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressController.createAddress(INVALID_UUID, address)
        );
    }

    @Test
    void updateCourtAddressReturns200() {
        CourtAddress address = new CourtAddress();
        when(courtAddressService.updateAddress(COURT_ID, ADDRESS_ID, address))
            .thenReturn(address);

        ResponseEntity<CourtAddress> response = courtAddressController.updateCourtAddress(
            COURT_ID.toString(),
            ADDRESS_ID.toString(),
            address
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void updateCourtAddressThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtAddress address = new CourtAddress();

        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressController.updateCourtAddress(INVALID_UUID, ADDRESS_ID.toString(), address)
        );
    }

    @Test
    void deleteCourtAddressReturns204() {
        ResponseEntity<Void> response = courtAddressController.deleteCourtAddress(
            COURT_ID.toString(),
            ADDRESS_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courtAddressService).deleteAddress(COURT_ID, ADDRESS_ID);
    }

    @Test
    void deleteCourtAddressThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressController.deleteCourtAddress(INVALID_UUID, ADDRESS_ID.toString())
        );
    }
}
