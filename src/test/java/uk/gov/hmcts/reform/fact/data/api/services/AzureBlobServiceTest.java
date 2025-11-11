package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobServiceTest {

    private static final String IMAGE_ID = "test-image-id";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final String BLOB_URL = "https://example.com/blob/test-image-id";

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AzureBlobService azureBlobService;

    @Test
    void uploadFileShouldUploadAndReturnUrl() throws IOException {
        byte[] fileBytes = "dummy payload".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(fileBytes);

        when(blobContainerClient.getBlobClient(IMAGE_ID)).thenReturn(blobClient);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getSize()).thenReturn((long) fileBytes.length);
        when(multipartFile.getContentType()).thenReturn(CONTENT_TYPE);
        when(blobClient.getBlobUrl()).thenReturn(BLOB_URL);

        String result = azureBlobService.uploadFile(IMAGE_ID, multipartFile);

        assertThat(result).isEqualTo(BLOB_URL);
        verify(blobClient).upload(inputStream, fileBytes.length, true);

        ArgumentCaptor<BlobHttpHeaders> headersCaptor = ArgumentCaptor.forClass(BlobHttpHeaders.class);
        verify(blobClient).setHttpHeaders(headersCaptor.capture());
        BlobHttpHeaders headers = headersCaptor.getValue();
        assertThat(headers.getContentType()).isEqualTo(CONTENT_TYPE);
    }

    @Test
    void uploadFileShouldThrowAzureUploadExceptionWhenReadingFails() throws IOException {
        when(blobContainerClient.getBlobClient(IMAGE_ID)).thenReturn(blobClient);
        when(multipartFile.getInputStream()).thenThrow(new IOException("Read failure"));

        assertThrows(AzureUploadException.class, () ->
            azureBlobService.uploadFile(IMAGE_ID, multipartFile)
        );

        verify(blobClient, never()).setHttpHeaders(org.mockito.ArgumentMatchers.any(BlobHttpHeaders.class));
        verify(blobClient, never()).getBlobUrl();
    }

    @Test
    void deleteBlobShouldDeleteBlobById() {
        when(blobContainerClient.getBlobClient(IMAGE_ID)).thenReturn(blobClient);

        azureBlobService.deleteBlob(IMAGE_ID);

        verify(blobClient).delete();
    }
}
