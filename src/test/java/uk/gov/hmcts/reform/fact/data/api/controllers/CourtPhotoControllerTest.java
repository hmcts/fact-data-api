package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtPhotoControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid-uuid";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtPhotoService courtPhotoService;

    @InjectMocks
    private CourtPhotoController courtPhotoController;

    @Test
    void getCourtPhotoReturns200() {
        CourtPhoto courtPhoto = new CourtPhoto();
        courtPhoto.setCourtId(COURT_ID);
        courtPhoto.setFileLink("https://example.com/photo.jpg");

        when(courtPhotoService.getCourtPhotoByCourtId(COURT_ID)).thenReturn(courtPhoto);

        ResponseEntity<CourtPhoto> response = courtPhotoController.getCourtPhotoByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courtPhoto);
    }

    @Test
    void getCourtPhotoThrowsNotFoundException() {
        when(courtPhotoService.getCourtPhotoByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court photo not found"));

        assertThrows(NotFoundException.class, () ->
            courtPhotoController.getCourtPhotoByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getCourtPhotoThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtPhotoController.getCourtPhotoByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setCourtPhotoReturns201() {
        MultipartFile file = mock(MultipartFile.class);

        CourtPhoto courtPhoto = new CourtPhoto();
        courtPhoto.setCourtId(COURT_ID);
        courtPhoto.setFileLink("https://example.com/photo.jpg");

        when(courtPhotoService.setCourtPhoto(COURT_ID, file)).thenReturn(courtPhoto);

        ResponseEntity<CourtPhoto> response = courtPhotoController.setCourtPhotoByCourtId(COURT_ID.toString(), file);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courtPhoto);
    }

    @Test
    void setCourtPhotoThrowsNotFoundException() {
        MultipartFile file = mock(MultipartFile.class);

        when(courtPhotoService.setCourtPhoto(UNKNOWN_COURT_ID, file))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtPhotoController.setCourtPhotoByCourtId(UNKNOWN_COURT_ID.toString(), file)
        );
    }

    @Test
    void setCourtPhotoThrowsIllegalArgumentExceptionForInvalidUUID() {
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(IllegalArgumentException.class, () ->
            courtPhotoController.setCourtPhotoByCourtId(INVALID_UUID, file)
        );
    }

    @Test
    void deleteCourtPhotoReturns204() {
        ResponseEntity<Void> response = courtPhotoController.deleteCourtPhotoByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isNull();
    }

    @Test
    void deleteCourtPhotoThrowsNotFoundException() {
        doThrow(new NotFoundException("Court photo not found"))
            .when(courtPhotoService).deleteCourtPhotoByCourtId(UNKNOWN_COURT_ID);

        assertThrows(NotFoundException.class, () ->
            courtPhotoController.deleteCourtPhotoByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void deleteCourtPhotoThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtPhotoController.deleteCourtPhotoByCourtId(INVALID_UUID)
        );
    }
}
