package uk.gov.hmcts.reform.fact.data.api.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfiguration {
    private static final String BLOB_ENDPOINT = "https://%s.blob.core.windows.net/";

    @Value("${azure.blob.tenant-id}")
    private String tenantId;

    @Value("${azure.managed-identity.client-id}")
    private String managedIdentityClientId;

    @Value("${azure.blob.container-name}")
    private String containerName;

    @Value("${azure.blob.connection-string}")
    private String connectionString;

    @Value("${azure.blob.storage-account-name}")
    private String storageAccountName;



    @Bean
    public BlobContainerClient blobContainerClient() {
        if (managedIdentityClientId.isEmpty()) {
            return new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient();
        }

        DefaultAzureCredential defaultCredential = new DefaultAzureCredentialBuilder()
            .tenantId(tenantId)
            .managedIdentityClientId(managedIdentityClientId)
            .build();

        return new BlobContainerClientBuilder()
            .endpoint(String.format(BLOB_ENDPOINT, storageAccountName))
            .credential(defaultCredential)
            .containerName(containerName)
            .buildClient();
    }
}
