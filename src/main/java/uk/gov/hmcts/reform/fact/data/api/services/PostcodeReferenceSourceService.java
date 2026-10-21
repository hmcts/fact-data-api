package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
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
import java.time.format.DateTimeFormatter;
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
        String blobPath = blobContainerClient.getBlobContainerName() + "/" + blobName;
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        if (storedSourceVersion(blobClient).filter(latestRelease.sourceVersion()::equals).isPresent()) {
            log.info("ONSPD release {} is already stored at {}", release, blobPath);
            return PostcodeReferenceSourceUpdateResult.unchanged(
                latestRelease.sourceVersion(), release, blobPath);
        }

        Path temporaryCsv = createTemporaryFile();
        try {
            long recordCount = csvService.createReducedCsv(latestRelease, temporaryCsv);
            blobClient.uploadFromFile(temporaryCsv.toString(), true);
            blobClient.setMetadata(Map.of(
                ITEM_ID_METADATA, latestRelease.itemId(),
                PostcodeReferenceImportService.SOURCE_VERSION_METADATA, latestRelease.sourceVersion(),
                PostcodeReferenceImportService.RELEASE_METADATA, release,
                PostcodeReferenceImportService.RECORD_COUNT_METADATA, Long.toString(recordCount)
            ));
            log.info("Uploaded ONSPD release {} with {} records to {}", release, recordCount, blobPath);
            return PostcodeReferenceSourceUpdateResult.updated(
                latestRelease.sourceVersion(), release, recordCount, blobPath);
        } catch (BlobStorageException ex) {
            throw new PostcodeReferenceImportException("Unable to upload ONSPD postcode reference blob", ex);
        } finally {
            deleteTemporaryFile(temporaryCsv);
        }
    }

    private Optional<String> storedSourceVersion(BlobClient blobClient) {
        try {
            return blobClient.getProperties().getMetadata().entrySet().stream()
                .filter(entry -> PostcodeReferenceImportService.SOURCE_VERSION_METADATA
                    .equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new PostcodeReferenceImportException("Unable to inspect ONSPD postcode reference blob", ex);
        }
    }

    private Path createTemporaryFile() {
        try {
            return Files.createTempFile("onspd-postcode-reference-", ".csv");
        } catch (IOException ex) {
            throw new PostcodeReferenceImportException("Unable to create temporary ONSPD CSV", ex);
        }
    }

    private void deleteTemporaryFile(Path temporaryCsv) {
        try {
            Files.deleteIfExists(temporaryCsv);
        } catch (IOException ex) {
            log.warn("Unable to delete temporary ONSPD CSV {}", temporaryCsv, ex);
        }
    }
}
