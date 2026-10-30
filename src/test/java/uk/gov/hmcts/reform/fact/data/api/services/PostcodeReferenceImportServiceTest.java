package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import com.azure.storage.blob.specialized.BlobInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportMetadata;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;
import uk.gov.hmcts.reform.fact.data.api.repositories.PostcodeReferenceImportRepository;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeReferenceImportServiceTest {

    private static final String BLOB_NAME = "postcode-reference.csv";

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @Mock
    private BlobInputStream blobInputStream;

    @Mock
    private PostcodeReferenceImportRepository importRepository;

    private PostcodeReferenceImportService importService;

    @BeforeEach
    void setUp() {
        importService = new PostcodeReferenceImportService(blobContainerClient, importRepository, BLOB_NAME);
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
    }

    @Test
    void importsBlobUsingItsMetadataAndEtag() {
        Instant importedAt = Instant.parse("2026-10-16T09:00:00Z");
        PostcodeReferenceImportResult expected = new PostcodeReferenceImportResult(
            true, "item-id:12345", "2026-08", 993_979, importedAt);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "item-id:12345",
            "onspdRelease", "2026-08",
            "recordCount", "993979"
        ));
        when(blobProperties.getETag()).thenReturn("etag-value");
        when(blobClient.openInputStream(any(BlobInputStreamOptions.class))).thenReturn(blobInputStream);
        when(importRepository.replaceIfRequired(any(), any())).thenReturn(expected);

        PostcodeReferenceImportResult result = importService.importLatest();

        assertThat(result).isEqualTo(expected);
        verify(importRepository).replaceIfRequired(
            blobInputStream,
            new PostcodeReferenceImportMetadata("item-id:12345", "2026-08", "etag-value", 993_979)
        );
        verify(blobInputStream).close();
    }

    @Test
    void rejectsBlobWithMissingMetadata() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of());

        assertThatThrownBy(importService::importLatest)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("onspdSourceVersion");
    }

    @Test
    void rejectsBlobWithInvalidRecordCount() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "item-id:12345",
            "onspdRelease", "2026-08",
            "recordCount", "not-a-number"
        ));

        assertThatThrownBy(importService::importLatest)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("Invalid ONSPD record count");
    }

    @ParameterizedTest
    @ValueSource(strings = {"August 2026", "2026-00", "2026-13"})
    void rejectsBlobWithInvalidRelease(String release) {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "item-id:12345",
            "onspdRelease", release,
            "recordCount", "993979"
        ));

        assertThatThrownBy(importService::importLatest)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("Invalid ONSPD release metadata");
    }

    @Test
    void rejectsBlobWithEmptyRecordCount() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "item-id:12345",
            "onspdRelease", "2026-08",
            "recordCount", "0"
        ));

        assertThatThrownBy(importService::importLatest)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("must be greater than zero");
    }

    @Test
    void reportsMissingBlobAsNotFound() {
        BlobStorageException exception = mock(BlobStorageException.class);
        when(exception.getStatusCode()).thenReturn(404);
        when(blobClient.getProperties()).thenThrow(exception);

        assertThatThrownBy(importService::importLatest)
            .isInstanceOf(NotFoundException.class)
            .hasMessage("ONSPD postcode reference blob was not found");
    }

    @Test
    void wrapsOtherBlobFailures() {
        BlobStorageException exception = mock(BlobStorageException.class);
        when(exception.getStatusCode()).thenReturn(500);
        when(blobClient.getProperties()).thenThrow(exception);

        assertThatThrownBy(importService::importLatest)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessage("Unable to read ONSPD postcode reference blob");
    }
}
