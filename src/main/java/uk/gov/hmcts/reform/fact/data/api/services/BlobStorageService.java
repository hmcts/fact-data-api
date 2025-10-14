package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BlobStorageService {

    private final BlobContainerClient blobContainerClient;

    public BlobStorageService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    public void logBlobInfo(String fileName) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

            if (!blobClient.exists()) {
                log.info("Blob '{}' does not exist in container '{}'",
                         fileName, blobContainerClient.getBlobContainerName());
            }

            BlobProperties properties = blobClient.getProperties();

            log.info("Blob Info for '{}':", fileName);
            log.info("  Container: {}", blobContainerClient.getBlobContainerName());
            log.info("  Size (bytes): {}", properties.getBlobSize());
            log.info("  Content type: {}", properties.getContentType());
            log.info("  Last modified: {}", properties.getLastModified());
            log.info("  ETag: {}", properties.getETag());
        } catch (Exception e) {
            log.error("Failed to get blob info for '{}'", fileName, e);
        }
    }
}

