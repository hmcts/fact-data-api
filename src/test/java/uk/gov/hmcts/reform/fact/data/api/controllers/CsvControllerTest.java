package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvControllerTest {

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CsvService csvService;

    @InjectMocks
    private CsvController csvController;

    @Test
    void createAndUploadCsvShouldDelegateToService() {
        csvController.createAndUploadCsv();

        verify(csvService).createAndUploadCsv();
    }

    @Test
    void downloadCsvReturns200WithAttachmentHeadersAndStreamBody() throws Exception {
        String csvContent = "header\nvalue";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(csvService.getCsvStreamInputStream()).thenReturn(inputStream);

        ResponseEntity<StreamingResponseBody> response = csvController.downloadCsv();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).as(RESPONSE_BODY_MESSAGE)
            .isEqualTo("attachment; filename=\"courts-and-tribunals-data.csv\"");
        assertThat(response.getHeaders().getContentType()).as(RESPONSE_BODY_MESSAGE)
            .isEqualTo(MediaType.parseMediaType("text/csv"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isNotNull();
        response.getBody().writeTo(outputStream);
        assertThat(outputStream.toString(StandardCharsets.UTF_8)).as(RESPONSE_BODY_MESSAGE).isEqualTo(csvContent);

        verify(csvService).getCsvStreamInputStream();
    }
}
