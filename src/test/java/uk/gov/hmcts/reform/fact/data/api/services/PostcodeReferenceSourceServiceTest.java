package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.clients.OnspdClient;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.OnspdRelease;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSourceUpdateResult;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeReferenceSourceServiceTest {

    @Mock
    private OnspdClient onspdClient;

    @Mock
    private PostcodeReferenceCsvService csvService;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    private PostcodeReferenceSourceService sourceService;
    private OnspdRelease release;

    @BeforeEach
    void setUp() {
        sourceService = new PostcodeReferenceSourceService(
            onspdClient, csvService, containerClient, "postcode-reference.csv");
        release = new OnspdRelease(
            "aug", "ONS Postcode Directory (August 2026) (Hosted Table)",
            YearMonth.of(2026, 8), "https://services.test/FeatureServer", 3L);
        when(onspdClient.findLatestRelease()).thenReturn(release);
        when(containerClient.getBlobContainerName()).thenReturn("onspd");
        when(containerClient.getBlobClient("postcode-reference.csv")).thenReturn(blobClient);
    }

    @Test
    void skipsDownloadWhenCurrentSourceVersionIsStored() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("OnspdSourceVersion", "aug:3"));

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isFalse();
        assertThat(result.release()).isEqualTo("2026-08");
        verify(csvService, never()).createReducedCsv(any(), any());
    }

    @Test
    void uploadsReducedCsvAndMetadataForNewRelease() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("onspdSourceVersion", "may:2"));
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(993_979L);

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isTrue();
        assertThat(result.recordCount()).isEqualTo(993_979);
        assertThat(result.blobPath()).isEqualTo("onspd/postcode-reference.csv");
        verify(blobClient).uploadFromFile(any(String.class), eq(true));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> metadata = ArgumentCaptor.forClass(Map.class);
        verify(blobClient).setMetadata(metadata.capture());
        assertThat(metadata.getValue())
            .containsEntry(PostcodeReferenceSourceService.ITEM_ID_METADATA, "aug")
            .containsEntry(PostcodeReferenceImportService.SOURCE_VERSION_METADATA, "aug:3")
            .containsEntry(PostcodeReferenceImportService.RELEASE_METADATA, "2026-08")
            .containsEntry(PostcodeReferenceImportService.RECORD_COUNT_METADATA, "993979");
    }

    @Test
    void treatsMissingBlobAsAReleaseThatNeedsUploading() {
        BlobStorageException missingBlob = mock(BlobStorageException.class);
        when(missingBlob.getStatusCode()).thenReturn(404);
        when(blobClient.getProperties()).thenThrow(missingBlob);
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(10L);

        assertThat(sourceService.updateIfRequired().updated()).isTrue();

        verify(blobClient).uploadFromFile(any(String.class), eq(true));
    }

    @Test
    void wrapsBlobInspectionFailures() {
        BlobStorageException failure = mock(BlobStorageException.class);
        when(failure.getStatusCode()).thenReturn(500);
        when(blobClient.getProperties()).thenThrow(failure);

        assertThatThrownBy(sourceService::updateIfRequired)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessage("Unable to inspect ONSPD postcode reference blob");
    }
}
