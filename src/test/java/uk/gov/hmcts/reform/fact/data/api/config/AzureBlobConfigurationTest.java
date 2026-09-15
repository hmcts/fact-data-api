package uk.gov.hmcts.reform.fact.data.api.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.services.AzureBlobService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureBlobConfigurationTest {

    private AzureBlobConfiguration azureBlobConfiguration;
    private BlobServiceClient mockBlobServiceClient;
    private BlobContainerClient mockBlobContainerClient;

    private final String containerName = "test-container";

    @BeforeEach
    void setUp() {
        azureBlobConfiguration = new AzureBlobConfiguration();
        mockBlobServiceClient = mock(BlobServiceClient.class);
        mockBlobContainerClient = mock(BlobContainerClient.class);

        when(mockBlobServiceClient.getBlobContainerClient(containerName))
            .thenReturn(mockBlobContainerClient);
    }

    @Test
    void testPhotoBlobContainerClientBean() {
        BlobContainerClient client = azureBlobConfiguration.photoBlobContainerClient(
            mockBlobServiceClient, containerName
        );

        assertEquals(mockBlobContainerClient, client);
        verify(mockBlobServiceClient, times(1))
            .getBlobContainerClient(containerName);
    }

    @Test
    void testCsvBlobContainerClientBean() {
        BlobContainerClient client = azureBlobConfiguration.csvBlobContainerClient(
            mockBlobServiceClient, containerName
        );

        assertEquals(mockBlobContainerClient, client);
        verify(mockBlobServiceClient, times(1))
            .getBlobContainerClient(containerName);
    }

    @Test
    void testPhotoAzureBlobServiceBean() {
        AzureBlobService service = azureBlobConfiguration.photoAzureBlobService(mockBlobContainerClient);

        assertThat(service).isNotNull();
    }

    @Test
    void testCsvAzureBlobServiceBean() {
        AzureBlobService service = azureBlobConfiguration.csvAzureBlobService(mockBlobContainerClient);

        assertThat(service).isNotNull();
    }
}
