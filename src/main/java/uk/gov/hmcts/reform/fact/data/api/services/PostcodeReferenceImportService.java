package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportMetadata;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;
import uk.gov.hmcts.reform.fact.data.api.repositories.PostcodeReferenceImportRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
public class PostcodeReferenceImportService {

    static final String SOURCE_VERSION_METADATA = "onspdSourceVersion";
    static final String RELEASE_METADATA = "onspdRelease";
    static final String RECORD_COUNT_METADATA = "recordCount";

    private final BlobContainerClient blobContainerClient;
    private final PostcodeReferenceImportRepository importRepository;
    private final String blobName;

    public PostcodeReferenceImportService(
        @Qualifier("onspdBlobContainerClient") BlobContainerClient blobContainerClient,
        PostcodeReferenceImportRepository importRepository,
        @Value("${onspd.blob-name}") String blobName
    ) {
        this.blobContainerClient = blobContainerClient;
        this.importRepository = importRepository;
        this.blobName = blobName;
    }

    public PostcodeReferenceImportResult importLatest() {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        try {
            BlobProperties properties = blobClient.getProperties();
            PostcodeReferenceImportMetadata metadata = toImportMetadata(properties);
            BlobRequestConditions conditions = new BlobRequestConditions().setIfMatch(properties.getETag());
            BlobInputStreamOptions options = new BlobInputStreamOptions().setRequestConditions(conditions);
            try (InputStream csvInputStream = blobClient.openInputStream(options)) {
                PostcodeReferenceImportResult result = importRepository.replaceIfRequired(csvInputStream, metadata);
                if (result.imported()) {
                    log.info(
                        "Imported ONSPD release {} with {} postcode reference records",
                        result.release(),
                        result.recordCount()
                    );
                } else {
                    log.info("ONSPD release {} has already been imported", result.release());
                }
                return result;
            }
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == 404) {
                throw new NotFoundException("ONSPD postcode reference blob was not found", ex);
            }
            throw new PostcodeReferenceImportException("Unable to read ONSPD postcode reference blob", ex);
        } catch (IOException ex) {
            throw new PostcodeReferenceImportException("Unable to close ONSPD postcode reference stream", ex);
        }
    }

    private PostcodeReferenceImportMetadata toImportMetadata(BlobProperties properties) {
        Map<String, String> metadata = properties.getMetadata();
        final String sourceVersion = requiredMetadata(metadata, SOURCE_VERSION_METADATA);
        final String release = requiredMetadata(metadata, RELEASE_METADATA);
        final String recordCount = requiredMetadata(metadata, RECORD_COUNT_METADATA);
        if (!release.matches("\\d{4}-\\d{2}")) {
            throw new PostcodeReferenceImportException("Invalid ONSPD release metadata: " + release);
        }

        long parsedRecordCount;
        try {
            parsedRecordCount = Long.parseLong(recordCount);
        } catch (NumberFormatException ex) {
            throw new PostcodeReferenceImportException("Invalid ONSPD record count metadata: " + recordCount, ex);
        }
        if (parsedRecordCount < 1) {
            throw new PostcodeReferenceImportException("ONSPD record count metadata must be greater than zero");
        }

        return new PostcodeReferenceImportMetadata(
            sourceVersion,
            release,
            properties.getETag(),
            parsedRecordCount
        );
    }

    private String requiredMetadata(Map<String, String> metadata, String name) {
        return metadata.entrySet().stream()
            .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
            .map(Map.Entry::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElseThrow(() -> new PostcodeReferenceImportException(
                "ONSPD blob is missing required metadata: " + name));
    }
}
