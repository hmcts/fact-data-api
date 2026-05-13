package uk.gov.hmcts.reform.fact.data.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.clients.SlackClient;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.fact.data.api.models.StringMultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    private static final String CSV_CONTAINER_NAME = "csv-container";
    private static final String CSV_FILE_NAME = "courts-and-tribunals-data.csv";

    @Mock
    private CourtService courtService;

    @Mock
    private CourtDetailsViewService courtDetailsViewService;

    @Mock
    private AzureBlobService azureBlobService;

    @Mock
    private SlackClient slackClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void uploadCsvToAzureBlobShouldUseConfiguredContainerName() {
        CsvService csvService = buildService();
        List<String> actions = new ArrayList<>();
        StringMultipartFile csvFile = new StringMultipartFile(CSV_FILE_NAME, CSV_FILE_NAME, "text/csv", "h1\nvalue");

        csvService.uploadCsvToAzureBlob(actions, csvFile);

        verify(azureBlobService).uploadFile(CSV_CONTAINER_NAME, CSV_FILE_NAME, csvFile);
        assertThat(actions).isEmpty();
    }

    @Test
    void uploadCsvToAzureBlobShouldAppendActionAndThrowWhenUploadFails() {
        CsvService csvService = buildService();
        List<String> actions = new ArrayList<>();
        StringMultipartFile csvFile = new StringMultipartFile(CSV_FILE_NAME, CSV_FILE_NAME, "text/csv", "h1\nvalue");

        doThrow(new RuntimeException("azure failure"))
            .when(azureBlobService)
            .uploadFile(CSV_CONTAINER_NAME, CSV_FILE_NAME, csvFile);

        AzureUploadException exception = assertThrows(AzureUploadException.class, () ->
            csvService.uploadCsvToAzureBlob(actions, csvFile)
        );

        assertThat(exception.getMessage()).isEqualTo("Failed to upload CSV file to Azure Blob Storage");
        assertThat(actions).containsExactly("Failed to upload CSV file to Azure Blob Storage. Check App insights.");
    }

    @Test
    void createAndUploadCsvShouldCreateAndUploadWithoutSlackMessageOnSuccess() {
        CsvService csvService = buildService();
        when(courtService.getAllCourtDetails()).thenReturn(Collections.emptyList());

        csvService.createAndUploadCsv();

        verify(azureBlobService).uploadFile(org.mockito.ArgumentMatchers.eq(CSV_CONTAINER_NAME),
            org.mockito.ArgumentMatchers.eq(CSV_FILE_NAME), any(StringMultipartFile.class));
        verify(slackClient, never()).sendSlackMessage(any());
    }

    @Test
    void createAndUploadCsvShouldSendSlackMessageAndThrowWhenCsvCreationFails() {
        CsvService csvService = buildService();
        when(courtService.getAllCourtDetails()).thenThrow(new RuntimeException("court failure"));

        CsvCreationException exception = assertThrows(CsvCreationException.class, csvService::createAndUploadCsv);

        assertThat(exception.getMessage()).isEqualTo("Failed to create CSV file");
        verify(slackClient).sendSlackMessage(contains("Failed to create CSV file. Check App insights."));
    }

    @Test
    void createAndUploadCsvShouldSendSlackMessageAndThrowWhenUploadFails() {
        CsvService csvService = buildService();
        when(courtService.getAllCourtDetails()).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("azure failure"))
            .when(azureBlobService)
            .uploadFile(org.mockito.ArgumentMatchers.eq(CSV_CONTAINER_NAME),
                org.mockito.ArgumentMatchers.eq(CSV_FILE_NAME), any(StringMultipartFile.class));

        AzureUploadException exception = assertThrows(AzureUploadException.class, csvService::createAndUploadCsv);

        assertThat(exception.getMessage()).isEqualTo("Failed to upload CSV file to Azure Blob Storage");
        verify(slackClient)
            .sendSlackMessage(contains("Failed to upload CSV file to Azure Blob Storage. Check App insights."));
    }

    private CsvService buildService() {
        return new CsvService(
            courtService, courtDetailsViewService, azureBlobService, objectMapper, slackClient, CSV_CONTAINER_NAME);
    }
}
