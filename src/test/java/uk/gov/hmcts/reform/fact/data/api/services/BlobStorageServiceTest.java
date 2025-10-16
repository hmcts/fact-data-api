package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlobStorageServiceTest {

    private BlobContainerClient mockContainerClient;
    private BlobClient mockBlobClient;
    private BlobProperties mockProperties;
    private AzureBlobService blobStorageService;

    private final String containerName = "test-container";
    private final String fileName = "test-file.txt";

    @BeforeEach
    void setUp() {
        mockContainerClient = mock(BlobContainerClient.class);
        mockBlobClient = mock(BlobClient.class);
        mockProperties = mock(BlobProperties.class);

        when(mockContainerClient.getBlobContainerName()).thenReturn(containerName);
        when(mockContainerClient.getBlobClient(fileName)).thenReturn(mockBlobClient);

        blobStorageService = new AzureBlobService(mockContainerClient);
    }

    @Test
    void testLogBlobInfo_blobExists() {
        // Arrange
        when(mockBlobClient.exists()).thenReturn(true);
        when(mockBlobClient.getProperties()).thenReturn(mockProperties);
        when(mockProperties.getBlobSize()).thenReturn(123L);
        when(mockProperties.getContentType()).thenReturn("text/plain");
        when(mockProperties.getLastModified()).thenReturn(OffsetDateTime.now());
        when(mockProperties.getETag()).thenReturn("etag123");

        // Act
        blobStorageService.logBlobInfo(fileName);

        // Assert
        verify(mockContainerClient, times(1)).getBlobClient(fileName);
        verify(mockBlobClient, times(1)).exists();
        verify(mockBlobClient, times(1)).getProperties();
    }

    @Test
    void testLogBlobInfo_exceptionHandling() {
        // Arrange
        when(mockContainerClient.getBlobClient(fileName)).thenThrow(new RuntimeException("test exception"));

        // Act
        blobStorageService.logBlobInfo(fileName);

        // Assert
        verify(mockContainerClient, times(1)).getBlobClient(fileName);
    }
}

