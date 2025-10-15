package uk.gov.hmcts.reform.fact.data.api.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class AzureBlobConfiguration {

    @Bean
    public BlobContainerClient blobContainerClient(
        BlobServiceClient blobServiceClient,
        @Value("${spring.cloud.azure.storage.blob.container-name}") String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }
}
