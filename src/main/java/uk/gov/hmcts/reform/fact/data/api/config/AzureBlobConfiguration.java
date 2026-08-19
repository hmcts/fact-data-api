package uk.gov.hmcts.reform.fact.data.api.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.gov.hmcts.reform.fact.data.api.services.AzureBlobService;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@Profile("!test")
public class AzureBlobConfiguration {

    @Bean
    public BlobContainerClient photoBlobContainerClient(
        BlobServiceClient blobServiceClient,
        @Value("${spring.cloud.azure.storage.blob.photo-container-name}") String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Bean
    public BlobContainerClient csvBlobContainerClient(
        BlobServiceClient blobServiceClient,
        @Value("${spring.cloud.azure.storage.blob.csv-container-name}") String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Bean
    public AzureBlobService photoAzureBlobService(
        @Qualifier("photoBlobContainerClient") BlobContainerClient photoBlobContainerClient) {
        return new AzureBlobService(photoBlobContainerClient);
    }

    @Bean
    public AzureBlobService csvAzureBlobService(
        @Qualifier("csvBlobContainerClient") BlobContainerClient csvBlobContainerClient) {
        return new AzureBlobService(csvBlobContainerClient);
    }
}
