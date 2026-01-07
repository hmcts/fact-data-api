package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAddressesService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAddressesControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid";

    @Mock
    private CourtAddressesService courtAddressesService;

    @InjectMocks
    private CourtAddressesController courtAddressesController;

    @Test
    void getAddressesReturns200() {
        CourtAddress address = new CourtAddress();
        when(courtAddressesService.getAddresses(COURT_ID)).thenReturn(List.of(address));

        ResponseEntity<List<CourtAddress>> response =
            courtAddressesController.getAddresses(COURT_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(address);
    }

    @Test
    void getAddressesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressesController.getAddresses(INVALID_UUID)
        );
    }

    @Test
    void getAddressReturns200() {
        CourtAddress address = new CourtAddress();
        when(courtAddressesService.getAddress(COURT_ID, ADDRESS_ID)).thenReturn(address);

        ResponseEntity<CourtAddress> response = courtAddressesController.getAddress(
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
                courtAddressesController.getAddress(INVALID_UUID, ADDRESS_ID.toString())
        );
    }

    @Test
    void createAddressReturns201() {
        CourtAddress address = new CourtAddress();
        when(courtAddressesService.createAddress(COURT_ID, address)).thenReturn(address);

        ResponseEntity<CourtAddress> response =
            courtAddressesController.createAddress(COURT_ID.toString(), address);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(address);
    }

    @Test
    void createAddressThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtAddress address = new CourtAddress();

        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressesController.createAddress(INVALID_UUID, address)
        );
    }

    @Test
    void updateCourtAddressReturns200() {
        CourtAddress address = new CourtAddress();
        when(courtAddressesService.updateAddress(COURT_ID, ADDRESS_ID, address))
            .thenReturn(address);

        ResponseEntity<CourtAddress> response = courtAddressesController.updateCourtAddress(
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
                courtAddressesController.updateCourtAddress(INVALID_UUID, ADDRESS_ID.toString(), address)
        );
    }

    @Test
    void deleteCourtAddressReturns204() {
        ResponseEntity<Void> response = courtAddressesController.deleteCourtAddress(
            COURT_ID.toString(),
            ADDRESS_ID.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courtAddressesService).deleteAddress(COURT_ID, ADDRESS_ID);
    }

    @Test
    void deleteCourtAddressThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtAddressesController.deleteCourtAddress(INVALID_UUID, ADDRESS_ID.toString())
        );
    }
}
