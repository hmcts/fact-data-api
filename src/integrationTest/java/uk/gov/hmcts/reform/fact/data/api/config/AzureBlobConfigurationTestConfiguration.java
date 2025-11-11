package uk.gov.hmcts.reform.fact.data.api.config;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class AzureBlobConfigurationTestConfiguration {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    public AzureBlobConfigurationTestConfiguration() {
        MockitoAnnotations.openMocks(this);
    }

    @Bean
    public BlobContainerClient blobContainerClient() {
        return containerClient;
    }

    @Bean
    public BlobClient blobClient() {
        return blobClient;
    }

    @Bean
    public BlobServiceClient blobServiceClient() {
        return blobServiceClient;
    }
}
