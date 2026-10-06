package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static uk.gov.hmcts.reform.fact.data.api.utils.LogBuilder.writeLog;

@Slf4j
@RequiredArgsConstructor
public class AzureBlobService {

    public record BlobBackup(byte[] content, String contentType) {}

    private final BlobContainerClient blobContainerClient;

    /**
     * Uploads a file to an Azure blob container.
     * If containerName is provided, it uploads to that container and creates it if it does not exist.
     *
     * @param blobName      The name of the blob to create.
     * @param file          The file to upload.
     * @return The URL of the uploaded blob.
     */
    public String uploadFile(String blobName, MultipartFile file) {

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        uploadToBlob(blobClient, file);

        log.debug(writeLog(
            "Uploaded file to Azure blob",
            "blobName=" + blobName,
            "hasOriginalFilename=" + (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()),
            "fileSize=" + file.getSize(),
            "hasBlobUrl=" + (blobClient.getBlobUrl() != null && !blobClient.getBlobUrl().isBlank())
        ));

        return blobClient.getBlobUrl();
    }

    private void uploadToBlob(BlobClient blobClient, MultipartFile file) {
        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType());
            blobClient.setHttpHeaders(headers);
        } catch (IOException e) {
            throw new AzureUploadException("Could not upload provided file to Azure");
        }
    }

    /**
     * Delete a blob from the blob store by the blob name.
     *
     * @param blobName The name of the blob to delete.
     */
    public void deleteBlob(String blobName) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        blobClient.delete();
    }

    /**
     * Capture the contents needed to restore a blob after an overwritten upload fails.
     *
     * @param blobName The name of the blob to back up.
     * @return The blob content and content type.
     */
    public BlobBackup backupBlob(String blobName) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        return new BlobBackup(
            blobClient.downloadContent().toBytes(),
            blobClient.getProperties().getContentType()
        );
    }

    /**
     * Restore a blob from an in-memory backup.
     *
     * @param blobName The name of the blob to restore.
     * @param backup The content to restore.
     */
    public void restoreBlob(String blobName, BlobBackup backup) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        byte[] content = backup.content();

        blobClient.upload(new ByteArrayInputStream(content), content.length, true);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(backup.contentType()));
    }

}
