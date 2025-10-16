package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;

import java.io.IOException;

@Service
public class AzureBlobService {

    private final BlobContainerClient blobContainerClient;

    public AzureBlobService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    //TODO: Get file ???

    /**
     * Uploads the image in the Azure blob service.
     *
     * @param imageId The identifier of the image
     * @param file  The file to upload
     * @return The id linked to the uploaded image
     */
    public String uploadFile(String imageId, MultipartFile file) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);

        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (IOException e) {
            throw new AzureUploadException("Could not upload provided file to Azure");
        }

        return imageId;
    }

    /**
     * Delete a blob from the blob store by the imageId.
     *
     * @param imageId The id of the blob to delete
     */
    public void deleteBlob(String imageId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);
        blobClient.delete();
    }
}
