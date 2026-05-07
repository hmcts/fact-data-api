package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;

import java.io.IOException;

@Slf4j
@Service
public class AzureBlobService {

    private final BlobContainerClient blobContainerClient;
    private final BlobServiceClient blobServiceClient;

    public AzureBlobService(BlobContainerClient blobContainerClient, BlobServiceClient blobServiceClient) {
        this.blobContainerClient = blobContainerClient;
        this.blobServiceClient = blobServiceClient;
    }

    /**
     * Uploads the image in the Azure blob service.
     *
     * @param imageId The identifier of the image.
     * @param file  The file to upload.
     * @return The id linked to the uploaded image.
     */
    public String uploadFile(String imageId, MultipartFile file) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);

        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType());

            blobClient.setHttpHeaders(headers);

        } catch (IOException e) {
            throw new AzureUploadException("Could not upload provided file to Azure");
        }

        return blobClient.getBlobUrl();
    }

    public void uploadFile(String containerName, String blobName, MultipartFile file) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("Created Azure blob container {}", containerName);
        }
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType());

            blobClient.setHttpHeaders(headers);
            log.info("Uploaded file {} to {}", file.getOriginalFilename(), containerName);

        } catch (IOException e) {
            throw new AzureUploadException("Could not upload provided file to Azure");
        }
    }

    /**
     * Delete a blob from the blob store by the imageId.
     *
     * @param imageId The identifier of the image.
     */
    public void deleteBlob(String imageId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);

        blobClient.delete();
    }
}
