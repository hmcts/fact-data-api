package uk.gov.hmcts.reform.fact.data.api.runners;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.data.api.services.AzureBlobService;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

@Component
@Slf4j
public class CsvGenerator implements CommandLineRunner {

    private final AzureBlobService azureService;
    private final CsvService csvService;

    public CsvGenerator(@Autowired AzureBlobService azureService,
                        @Autowired CsvService csvService) {
        this.azureService = azureService;
        this.csvService = csvService;
    }

    /**
     * Executes automatically when the Spring Boot application starts.
     * This method is invoked as part of the application's startup process due to
     * the implementation of the {@link CommandLineRunner} interface. At present, it triggers
     * the generation of court and tribunal data in CSV format and uploads it to
     * Azure Blob Storage. The method logs the start and completion of this process.
     *
     * @param args Optional command-line arguments passed during application startup.
     */
    @Override
    public void run(String... args) {
        log.info("Running CSV generation");
        createCsvAndUpload();
        log.info("Finished running CSV generation");
        System.exit(0);
    }

    public void createCsvAndUpload() {
        JsonNode courtData = csvService.convertCourtDataToJson();
        azureService.createCsvFileAndUpload("csv", "courts-and-tribunals-data.csv", courtData);
    }
}
