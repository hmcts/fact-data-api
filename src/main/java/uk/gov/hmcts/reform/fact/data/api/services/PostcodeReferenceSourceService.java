package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.clients.OnspdClient;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.OnspdRelease;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSourceUpdateResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class PostcodeReferenceSourceService {

    static final String ITEM_ID_METADATA = "onspdItemId";

    private static final DateTimeFormatter RELEASE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OnspdClient onspdClient;
    private final PostcodeReferenceCsvService csvService;
    private final BlobContainerClient blobContainerClient;
    private final String blobName;

    public PostcodeReferenceSourceService(
        OnspdClient onspdClient,
        PostcodeReferenceCsvService csvService,
        @Qualifier("onspdBlobContainerClient") BlobContainerClient blobContainerClient,
        @Value("${onspd.blob-name}") String blobName
    ) {
        this.onspdClient = onspdClient;
        this.csvService = csvService;
        this.blobContainerClient = blobContainerClient;
        this.blobName = blobName;
    }

    /**
     * Replaces the reduced blob only when ONS has published a different source version.
     *
     * @return source update outcome used by the synchronisation response
     */
    public PostcodeReferenceSourceUpdateResult updateIfRequired() {
        OnspdRelease latestRelease = onspdClient.findLatestRelease();
        String release = latestRelease.releaseDate().format(RELEASE_FORMAT);
        String blobPath = Path.of(blobContainerClient.getBlobContainerName(), blobName).toString();
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        Optional<StoredBlob> storedBlob = inspectStoredBlob(blobClient);
        Optional<StoredBlob> sameOrNewerBlob = storedBlob.filter(blob -> blob.isSameOrNewerThan(latestRelease));
        if (sameOrNewerBlob.isPresent()) {
            return unchangedResult(sameOrNewerBlob.get(), latestRelease, blobPath, false);
        }

        Path temporaryCsv = createTemporaryFile();
        try {
            long recordCount = csvService.createReducedCsv(latestRelease, temporaryCsv);
            Map<String, String> metadata = Map.of(
                ITEM_ID_METADATA, latestRelease.itemId(),
                PostcodeReferenceImportService.SOURCE_VERSION_METADATA, latestRelease.sourceVersion(),
                PostcodeReferenceImportService.RELEASE_METADATA, release,
                PostcodeReferenceImportService.RECORD_COUNT_METADATA, Long.toString(recordCount)
            );
            BlobRequestConditions conditions = storedBlob
                .map(blob -> new BlobRequestConditions().setIfMatch(blob.eTag()))
                .orElseGet(() -> new BlobRequestConditions().setIfNoneMatch("*"));
            BlobUploadFromFileOptions uploadOptions = new BlobUploadFromFileOptions(temporaryCsv.toString())
                .setMetadata(metadata)
                .setRequestConditions(conditions);
            blobClient.uploadFromFileWithResponse(uploadOptions, null, Context.NONE);
            log.info("Uploaded ONSPD release {} with {} records to {}", release, recordCount, blobPath);
            return PostcodeReferenceSourceUpdateResult.updated(
                latestRelease.sourceVersion(), release, recordCount, blobPath);
        } catch (BlobStorageException ex) {
            Optional<StoredBlob> sameOrNewerPublishedBlob = ex.getStatusCode() == 412
                ? inspectStoredBlob(blobClient)
                    .filter(blob -> blob.isSameOrNewerThan(latestRelease))
                : Optional.empty();
            if (sameOrNewerPublishedBlob.isPresent()) {
                return unchangedResult(sameOrNewerPublishedBlob.get(), latestRelease, blobPath, true);
            }
            throw new PostcodeReferenceImportException("Unable to upload ONSPD postcode reference blob", ex);
        } finally {
            deleteTemporaryFile(temporaryCsv);
        }
    }

    private Optional<StoredBlob> inspectStoredBlob(BlobClient blobClient) {
        try {
            BlobProperties properties = blobClient.getProperties();
            Optional<String> sourceVersion = properties.getMetadata().entrySet().stream()
                .filter(entry -> PostcodeReferenceImportService.SOURCE_VERSION_METADATA
                    .equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
            Optional<YearMonth> release = properties.getMetadata().entrySet().stream()
                .filter(entry -> PostcodeReferenceImportService.RELEASE_METADATA.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(this::parseRelease)
                .findFirst();
            return Optional.of(new StoredBlob(sourceVersion, release, properties.getETag()));
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new PostcodeReferenceImportException("Unable to inspect ONSPD postcode reference blob", ex);
        }
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile(
                "onspd-postcode-reference-",
                ".csv",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            );
        } catch (IOException ex) {
            throw new PostcodeReferenceImportException("Unable to create temporary ONSPD CSV", ex);
        }
    }

    private YearMonth parseRelease(String release) {
        try {
            return YearMonth.parse(release, RELEASE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new PostcodeReferenceImportException("Invalid ONSPD release metadata: " + release, ex);
        }
    }

    private PostcodeReferenceSourceUpdateResult unchangedResult(
        StoredBlob storedBlob,
        OnspdRelease discoveredRelease,
        String blobPath,
        boolean concurrentlyPublished
    ) {
        String storedRelease = storedBlob.release()
            .map(RELEASE_FORMAT::format)
            .orElseGet(() -> discoveredRelease.releaseDate().format(RELEASE_FORMAT));
        if (concurrentlyPublished) {
            log.info(
                "ONSPD release {} was stored by another synchronisation at {}",
                storedRelease,
                blobPath
            );
        } else {
            log.info("ONSPD release {} is already stored at {}", storedRelease, blobPath);
        }
        return PostcodeReferenceSourceUpdateResult.unchanged(
            storedBlob.sourceVersion().orElse(discoveredRelease.sourceVersion()),
            storedRelease,
            blobPath
        );
    }

    private void deleteTemporaryFile(Path temporaryCsv) {
        try {
            Files.deleteIfExists(temporaryCsv);
        } catch (IOException ex) {
            log.warn("Unable to delete temporary ONSPD CSV {}", temporaryCsv, ex);
        }
    }

    private record StoredBlob(Optional<String> sourceVersion, Optional<YearMonth> release, String eTag) {

        private boolean isSameOrNewerThan(OnspdRelease discoveredRelease) {
            if (sourceVersion.filter(discoveredRelease.sourceVersion()::equals).isPresent()) {
                return true;
            }

            if (release.isPresent()) {
                int releaseOrder = release.get().compareTo(discoveredRelease.releaseDate());
                if (releaseOrder != 0) {
                    return releaseOrder > 0;
                }
            }

            return sourceVersion
                .flatMap(StoredBlob::modifiedTimestamp)
                .filter(modified -> modified >= discoveredRelease.modified())
                .isPresent();
        }

        private static Optional<Long> modifiedTimestamp(String sourceVersion) {
            int delimiterIndex = sourceVersion.lastIndexOf(':');
            if (delimiterIndex < 0 || delimiterIndex == sourceVersion.length() - 1) {
                return Optional.empty();
            }
            try {
                return Optional.of(Long.parseLong(sourceVersion.substring(delimiterIndex + 1)));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
    }
}
