package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    void replacesBlobWhenStoredSourceVersionHasInvalidModifiedTimestamp() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("onspdSourceVersion", "aug:not-a-number"));
        when(blobProperties.getETag()).thenReturn("old-etag");
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(10L);

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isTrue();
        verify(blobClient).uploadFromFileWithResponse(any(), isNull(), eq(Context.NONE));
    }

    @Test
    void uploadsReducedCsvAndMetadataForNewRelease() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("onspdSourceVersion", "may:2"));
        when(blobProperties.getETag()).thenReturn("old-etag");
        AtomicReference<Path> temporaryCsv = new AtomicReference<>();
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenAnswer(invocation -> {
            Path csvPath = invocation.getArgument(1);
            temporaryCsv.set(csvPath);
            assertThat(Files.getPosixFilePermissions(csvPath)).containsExactlyInAnyOrder(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            return 993_979L;
        });

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isTrue();
        assertThat(result.recordCount()).isEqualTo(993_979);
        assertThat(result.blobPath()).isEqualTo("onspd/postcode-reference.csv");
        assertThat(temporaryCsv.get()).doesNotExist();
        ArgumentCaptor<BlobUploadFromFileOptions> uploadOptions =
            ArgumentCaptor.forClass(BlobUploadFromFileOptions.class);
        verify(blobClient).uploadFromFileWithResponse(uploadOptions.capture(), isNull(), eq(Context.NONE));
        assertThat(uploadOptions.getValue().getMetadata())
            .containsEntry(PostcodeReferenceSourceService.ITEM_ID_METADATA, "aug")
            .containsEntry(PostcodeReferenceImportService.SOURCE_VERSION_METADATA, "aug:3")
            .containsEntry(PostcodeReferenceImportService.RELEASE_METADATA, "2026-08")
            .containsEntry(PostcodeReferenceImportService.RECORD_COUNT_METADATA, "993979");
        assertThat(uploadOptions.getValue().getRequestConditions().getIfMatch()).isEqualTo("old-etag");
    }

    @Test
    void treatsMissingBlobAsAReleaseThatNeedsUploading() {
        BlobStorageException missingBlob = mock(BlobStorageException.class);
        when(missingBlob.getStatusCode()).thenReturn(404);
        when(blobClient.getProperties()).thenThrow(missingBlob);
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(10L);

        assertThat(sourceService.updateIfRequired().updated()).isTrue();

        ArgumentCaptor<BlobUploadFromFileOptions> uploadOptions =
            ArgumentCaptor.forClass(BlobUploadFromFileOptions.class);
        verify(blobClient).uploadFromFileWithResponse(uploadOptions.capture(), isNull(), eq(Context.NONE));
        assertThat(uploadOptions.getValue().getRequestConditions().getIfNoneMatch()).isEqualTo("*");
    }

    @Test
    void treatsConcurrentPublicationOfTheSameReleaseAsUnchanged() {
        BlobProperties previousProperties = mock(BlobProperties.class);
        BlobProperties currentProperties = mock(BlobProperties.class);
        when(previousProperties.getMetadata()).thenReturn(Map.of("onspdSourceVersion", "may:2"));
        when(previousProperties.getETag()).thenReturn("old-etag");
        when(currentProperties.getMetadata()).thenReturn(Map.of("onspdSourceVersion", "aug:3"));
        when(currentProperties.getETag()).thenReturn("new-etag");
        when(blobClient.getProperties()).thenReturn(previousProperties, currentProperties);
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(10L);
        BlobStorageException preconditionFailed = mock(BlobStorageException.class);
        when(preconditionFailed.getStatusCode()).thenReturn(412);
        when(blobClient.uploadFromFileWithResponse(any(), isNull(), eq(Context.NONE)))
            .thenThrow(preconditionFailed);

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isFalse();
        assertThat(result.sourceVersion()).isEqualTo("aug:3");
    }

    @Test
    void preventsAnOlderReleaseOverwritingANewerConcurrentPublication() {
        OnspdRelease mayRelease = new OnspdRelease(
            "may", "ONS Postcode Directory (May 2026) (Hosted Table)",
            YearMonth.of(2026, 5), "https://services.test/FeatureServer", 2L);
        when(onspdClient.findLatestRelease()).thenReturn(mayRelease);
        BlobProperties previousProperties = mock(BlobProperties.class);
        BlobProperties currentProperties = mock(BlobProperties.class);
        when(previousProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "feb:1",
            "onspdRelease", "2026-02"
        ));
        when(previousProperties.getETag()).thenReturn("old-etag");
        when(currentProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "aug:3",
            "onspdRelease", "2026-08"
        ));
        when(currentProperties.getETag()).thenReturn("new-etag");
        when(blobClient.getProperties()).thenReturn(previousProperties, currentProperties);
        when(csvService.createReducedCsv(eq(mayRelease), any(Path.class))).thenReturn(10L);
        BlobStorageException preconditionFailed = mock(BlobStorageException.class);
        when(preconditionFailed.getStatusCode()).thenReturn(412);
        when(blobClient.uploadFromFileWithResponse(any(), isNull(), eq(Context.NONE)))
            .thenThrow(preconditionFailed);

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isFalse();
        assertThat(result.sourceVersion()).isEqualTo("aug:3");
        assertThat(result.release()).isEqualTo("2026-08");
    }

    @Test
    void skipsDownloadWhenStorageAlreadyContainsANewerRelease() {
        OnspdRelease mayRelease = new OnspdRelease(
            "may", "ONS Postcode Directory (May 2026) (Hosted Table)",
            YearMonth.of(2026, 5), "https://services.test/FeatureServer", 2L);
        when(onspdClient.findLatestRelease()).thenReturn(mayRelease);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of(
            "onspdSourceVersion", "aug:3",
            "onspdRelease", "2026-08"
        ));

        PostcodeReferenceSourceUpdateResult result = sourceService.updateIfRequired();

        assertThat(result.updated()).isFalse();
        assertThat(result.sourceVersion()).isEqualTo("aug:3");
        assertThat(result.release()).isEqualTo("2026-08");
        verify(csvService, never()).createReducedCsv(any(), any());
    }

    @Test
    void wrapsBlobUploadFailures() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("onspdSourceVersion", "may:2"));
        when(blobProperties.getETag()).thenReturn("old-etag");
        when(csvService.createReducedCsv(eq(release), any(Path.class))).thenReturn(10L);
        BlobStorageException uploadFailure = mock(BlobStorageException.class);
        when(uploadFailure.getStatusCode()).thenReturn(500);
        when(blobClient.uploadFromFileWithResponse(any(), isNull(), eq(Context.NONE)))
            .thenThrow(uploadFailure);

        assertThatThrownBy(sourceService::updateIfRequired)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessage("Unable to upload ONSPD postcode reference blob");
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
