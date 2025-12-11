package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtSinglePointsOfEntryService;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CourtSinglePointsOfEntryControllerTest {

    UUID courtId = UUID.randomUUID();

    static AreaOfLawType adoption = AreaOfLawType.builder().id(UUID.randomUUID()).name("Adoption").build();

    @Mock
    CourtSinglePointsOfEntryService courtSinglePointOfEntryService;

    @InjectMocks
    private CourtSinglePointsOfEntryController courtSinglePointOfEntryController;

    @Test
    void getCourtSinglePointsOfEntryReturnsValidResponse() {
        List<AreaOfLawSelectionDto> result = List.of(AreaOfLawSelectionDto.asSelected(adoption));

        when(courtSinglePointOfEntryService.getCourtSinglePointsOfEntry(courtId)).thenReturn(result);

        ResponseEntity<List<AreaOfLawSelectionDto>> response =
            courtSinglePointOfEntryController.getSinglePointsOfEntry(courtId.toString());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
    }

    @Test
    void getCourtSinglePointsOfThrowsNotFoundException() {
        when(courtSinglePointOfEntryService.getCourtSinglePointsOfEntry(courtId)).thenThrow(NotFoundException.class);
        String id = courtId.toString();
        assertThrows(NotFoundException.class, () -> courtSinglePointOfEntryController.getSinglePointsOfEntry(id));
    }

    @Test
    void getCourntSinglePointsOfEntryThrowsIllegalArgumentExceptionWhenCourtIdIsInvalid() {
        assertThrows(
            IllegalArgumentException.class,
            () -> courtSinglePointOfEntryController.getSinglePointsOfEntry("invalid")
        );
    }

    @Test
    void updateCourtSinglePointsOfEntryReturnsValidResponse() {
        List<AreaOfLawSelectionDto> updates = List.of(AreaOfLawSelectionDto.asSelected(adoption));

        doNothing().when(courtSinglePointOfEntryService).updateCourtSinglePointsOfEntry(courtId, updates);

        ResponseEntity<Void> response =
            courtSinglePointOfEntryController.updateSinglePointsOfEntry(courtId.toString(), updates);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateCourtSinglePointsOfThrowsNotFoundException() {
        List<AreaOfLawSelectionDto> updates = List.of(AreaOfLawSelectionDto.asSelected(adoption));
        doThrow(NotFoundException.class).when(courtSinglePointOfEntryService).updateCourtSinglePointsOfEntry(
            courtId,
            updates
        );
        String id = courtId.toString();
        assertThrows(
            NotFoundException.class,
            () -> courtSinglePointOfEntryController.updateSinglePointsOfEntry(id, updates)
        );
    }

    @Test
    void updateCourntSinglePointsOfEntryThrowsIllegalArgumentExceptionWhenCourtIdIsInvalid() {
        List<AreaOfLawSelectionDto> updates = List.of(AreaOfLawSelectionDto.asSelected(adoption));
        assertThrows(
            IllegalArgumentException.class,
            () -> courtSinglePointOfEntryController.updateSinglePointsOfEntry("invalid", updates)
        );
    }


}
