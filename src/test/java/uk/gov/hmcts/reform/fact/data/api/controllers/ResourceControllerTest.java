package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();

    @Mock
    private CourtPhotoService courtPhotoService;

    @Mock
    private CsvService csvService;

    @InjectMocks
    private ResourceController resourceController;

    @ParameterizedTest
    @ValueSource(strings = {"image/jpg", "image/jpeg", "image/png"})
    void downloadPhotoReturns200WithContentTypeAndStream(String contentType) {
        StreamingResponseBody body = out -> out.write(new byte[] {1, 2, 3});
        when(courtPhotoService.getPhotoStream(COURT_ID))
            .thenReturn(new CourtPhotoService.PhotoStreamDetails(contentType, body));

        ResponseEntity<StreamingResponseBody> response = resourceController.downloadPhoto(COURT_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType(contentType));
        assertThat(response.getBody()).isSameAs(body);
        verify(courtPhotoService).getPhotoStream(COURT_ID);
    }

    @Test
    void downloadPhotoThrowsNotFoundException() {
        NotFoundException exception = new NotFoundException("Court photo not found");
        when(courtPhotoService.getPhotoStream(COURT_ID)).thenThrow(exception);
        String courtId = COURT_ID.toString();

        assertThat(assertThrows(NotFoundException.class, () -> resourceController.downloadPhoto(courtId)))
            .isSameAs(exception);
    }

    @Test
    void downloadPhotoThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () -> resourceController.downloadPhoto("invalid-uuid"));

        verifyNoInteractions(courtPhotoService);
    }

    @Test
    void downloadCsvReturns200WithDownloadHeadersAndStream() {
        StreamingResponseBody body = out -> out.write(new byte[] {1, 2, 3});
        when(csvService.getCsvStreamInputStream()).thenReturn(body);

        ResponseEntity<StreamingResponseBody> response = resourceController.downloadCsv();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("text/csv"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename=\"courts-and-tribunals-data.csv\"");
        assertThat(response.getBody()).isSameAs(body);
        verify(csvService).getCsvStreamInputStream();
    }

    @Test
    void downloadCsvThrowsNotFoundException() {
        NotFoundException exception = new NotFoundException("CSV file not found");
        when(csvService.getCsvStreamInputStream()).thenThrow(exception);

        assertThat(assertThrows(NotFoundException.class, resourceController::downloadCsv)).isSameAs(exception);
    }
}
