package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAreasOfLawService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAreasOfLawControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtAreasOfLawService courtAreasOfLawService;

    @InjectMocks
    private CourtAreasOfLawController courtAreasOfLawController;

    @Test
    void getAreasOfLawReturns200() {
        Map<AreaOfLawType, Boolean> areasOfLaw = new HashMap<>();
        areasOfLaw.put(new AreaOfLawType(), true);

        when(courtAreasOfLawService.getAreasOfLawStatusByCourtId(COURT_ID)).thenReturn(areasOfLaw);

        ResponseEntity<Map<AreaOfLawType, Boolean>> response = courtAreasOfLawController
            .getAreasOfLawByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(areasOfLaw);
    }

    @Test
    void getAreasOfLawThrowsNotFoundException() {
        when(courtAreasOfLawService.getAreasOfLawStatusByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        String unknownCourtId = UNKNOWN_COURT_ID.toString();

        assertThrows(
            NotFoundException.class, () ->
                courtAreasOfLawController.getAreasOfLawByCourtId(unknownCourtId)
        );
    }

    @Test
    void getAreasOfLawThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtAreasOfLawController.getAreasOfLawByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setAreasOfLawReturns201() {
        CourtAreasOfLaw areasOfLaw = new CourtAreasOfLaw();
        areasOfLaw.setCourtId(COURT_ID);

        when(courtAreasOfLawService.setCourtAreasOfLaw(COURT_ID, areasOfLaw)).thenReturn(areasOfLaw);

        ResponseEntity<CourtAreasOfLaw> response = courtAreasOfLawController
            .setAreasOfLawServices(COURT_ID.toString(), areasOfLaw);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(areasOfLaw);
    }

    @Test
    void setAreasOfLawThrowsNotFoundException() {
        CourtAreasOfLaw areasOfLaw = new CourtAreasOfLaw();
        areasOfLaw.setCourtId(UNKNOWN_COURT_ID);

        when(courtAreasOfLawService.setCourtAreasOfLaw(UNKNOWN_COURT_ID, areasOfLaw))
            .thenThrow(new NotFoundException("Court not found"));

        String unknownCourtId = UNKNOWN_COURT_ID.toString();

        assertThrows(
            NotFoundException.class, () ->
                courtAreasOfLawController.setAreasOfLawServices(unknownCourtId, areasOfLaw)
        );
    }

    @Test
    void setAreasOfLawThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtAreasOfLaw areasOfLaw = new CourtAreasOfLaw();

        assertThrows(
            IllegalArgumentException.class, () ->
                courtAreasOfLawController.setAreasOfLawServices(INVALID_UUID, areasOfLaw)
        );
    }
}
