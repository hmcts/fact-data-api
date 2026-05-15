package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;

import java.io.IOException;

@Slf4j
public class AzureBlobService {

    private final BlobContainerClient blobContainerClient;

    public AzureBlobService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

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

        log.info("Uploaded file {} to {}", file.getOriginalFilename(), blobClient.getBlobUrl());

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
}
