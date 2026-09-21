package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobInputStream;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.gov.hmcts.reform.fact.data.api.clients.SlackClient;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.StringMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    private static final String CSV_FILE_NAME = "courts-and-tribunals-data.csv";

    @Mock
    private CourtService courtService;

    @Mock
    private CourtDetailsViewService courtDetailsViewService;

    @Mock
    private ServiceCentreService serviceCentreService;

    @Mock
    private ServiceCentreDetailsViewService serviceCentreDetailsViewService;

    @Mock
    private AzureBlobService azureBlobService;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private SlackClient slackClient;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void uploadCsvToAzureBlobShouldUseConfiguredContainerName() {
        CsvService csvService = buildService();
        List<String> actions = new ArrayList<>();
        StringMultipartFile csvFile = new StringMultipartFile(CSV_FILE_NAME, CSV_FILE_NAME, "text/csv", "h1\nvalue");

        csvService.uploadCsvToAzureBlob(actions, csvFile);

        verify(azureBlobService).uploadFile(CSV_FILE_NAME, csvFile);
        assertThat(actions).isEmpty();
    }

    @Test
    void uploadCsvToAzureBlobShouldAppendActionAndThrowWhenUploadFails() {
        CsvService csvService = buildService();
        List<String> actions = new ArrayList<>();
        StringMultipartFile csvFile = new StringMultipartFile(CSV_FILE_NAME, CSV_FILE_NAME, "text/csv", "h1\nvalue");

        doThrow(new RuntimeException("azure failure"))
            .when(azureBlobService)
            .uploadFile(CSV_FILE_NAME, csvFile);

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
        when(serviceCentreService.getAllServiceCentreDetails()).thenReturn(Collections.emptyList());

        csvService.createAndUploadCsv();

        verify(azureBlobService)
            .uploadFile(org.mockito.ArgumentMatchers.eq(CSV_FILE_NAME), any(StringMultipartFile.class));
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
        when(serviceCentreService.getAllServiceCentreDetails()).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("azure failure"))
            .when(azureBlobService)
            .uploadFile(org.mockito.ArgumentMatchers.eq(CSV_FILE_NAME), any(StringMultipartFile.class));

        AzureUploadException exception = assertThrows(AzureUploadException.class, csvService::createAndUploadCsv);

        assertThat(exception.getMessage()).isEqualTo("Failed to upload CSV file to Azure Blob Storage");
        verify(slackClient)
            .sendSlackMessage(contains("Failed to upload CSV file to Azure Blob Storage. Check App insights."));
    }

    @Test
    void getCsvStreamInputStreamShouldStreamContentAndCloseInputStream() throws IOException {
        BlobClient blobClient = mock(BlobClient.class);
        BlobInputStream inputStream = mock(BlobInputStream.class);
        byte[] content = {1, 2, 3};
        when(blobContainerClient.getBlobClient(CSV_FILE_NAME)).thenReturn(blobClient);
        when(blobClient.openInputStream()).thenReturn(inputStream);
        when(inputStream.transferTo(any(OutputStream.class))).thenAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(content);
            return (long) content.length;
        });

        StreamingResponseBody body = buildService().getCsvStreamInputStream();

        verify(inputStream, never()).close();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        body.writeTo(outputStream);

        assertThat(outputStream.toByteArray()).isEqualTo(content);
        verify(inputStream).close();
    }

    @Test
    void getCsvStreamInputStreamShouldCloseInputStreamWhenTransferFails() throws IOException {
        BlobClient blobClient = mock(BlobClient.class);
        BlobInputStream inputStream = mock(BlobInputStream.class);
        IOException failure = new IOException("transfer failed");
        when(blobContainerClient.getBlobClient(CSV_FILE_NAME)).thenReturn(blobClient);
        when(blobClient.openInputStream()).thenReturn(inputStream);
        when(inputStream.transferTo(any(OutputStream.class))).thenThrow(failure);

        StreamingResponseBody body = buildService().getCsvStreamInputStream();

        assertThat(assertThrows(IOException.class, () -> body.writeTo(new ByteArrayOutputStream())))
            .isSameAs(failure);
        verify(inputStream).close();
    }

    @Test
    void getCsvStreamInputStreamShouldThrowNotFoundWhenBlobCannotBeOpened() {
        CsvService svc = buildService();
        BlobClient blobClient = mock(BlobClient.class);
        BlobStorageException failure = mock(BlobStorageException.class);
        when(blobContainerClient.getBlobClient(CSV_FILE_NAME)).thenReturn(blobClient);
        when(blobClient.openInputStream()).thenThrow(failure);

        NotFoundException exception = assertThrows(NotFoundException.class, svc::getCsvStreamInputStream);

        assertThat(exception.getMessage()).isEqualTo("CSV file not downloaded from Azure Blob Storage");
        assertThat(exception.getCause()).isSameAs(failure);
    }

    private CsvService buildService() {
        return new CsvService(
            courtService,
            courtDetailsViewService,
            serviceCentreService,
            serviceCentreDetailsViewService,
            azureBlobService,
            blobContainerClient,
            objectMapper,
            slackClient
        );
    }
}
