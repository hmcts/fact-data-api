package uk.gov.hmcts.reform.fact.data.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.fact.data.api.clients.SlackClient;
import uk.gov.hmcts.reform.fact.data.api.models.StringMultipartFile;
import uk.gov.hmcts.reform.fact.data.api.utils.CsvUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling CSV file operations.
 */
@Slf4j
@Service
public class CsvService {

    private static final String CSV_FILE_NAME = "courts-and-tribunals-data.csv";
    private static final String CSV_CONTENT_TYPE = "text/csv";

    private final String csvContainerName;
    private final CourtService courtService;
    private final CourtDetailsViewService courtDetailsViewService;
    private final AzureBlobService azureBlobService;
    private final ObjectMapper objectMapper;
    private final SlackClient slackClient;

    public CsvService(CourtService courtService,
                      CourtDetailsViewService courtDetailsViewService,
                      AzureBlobService azureBlobService,
                      ObjectMapper objectMapper,
                      SlackClient slackClient,
                      @Value("${fact.data-api.csv-container-name:csv}") String csvContainerName) {
        this.courtService = courtService;
        this.courtDetailsViewService = courtDetailsViewService;
        this.azureBlobService = azureBlobService;
        this.objectMapper = objectMapper;
        this.slackClient = slackClient;
        this.csvContainerName = csvContainerName;
    }

    public void createAndUploadCsv() {
        List<String> actions = new ArrayList<>();
        try {
            StringMultipartFile csvFile = createCsvFile(actions);
            uploadCsvToAzureBlob(actions, csvFile);
        } finally {
            sendSlackSummary(actions);
        }
    }

    public void uploadCsvToAzureBlob(List<String> actions, StringMultipartFile stringMultipartFile) {
        try {
            azureBlobService
                .uploadFile(csvContainerName, CSV_FILE_NAME, stringMultipartFile);
        } catch (Exception e) {
            log.error("Error while uploading CSV", e);
            actions.add("Failed to upload CSV file to Azure Blob Storage. Check App insights.");
            throw new RuntimeException("Failed to upload CSV file to Azure Blob Storage", e);
        }
    }

    public StringMultipartFile createCsvFile(List<String> actions) {
        try {
            return new StringMultipartFile(
                CSV_FILE_NAME,
                CSV_FILE_NAME,
                CSV_CONTENT_TYPE,
                new CsvUtil()
                    .convertJsonToCsv(
                        objectMapper.valueToTree(
                            courtService.getAllCourtDetails().stream().map(
                                courtDetailsViewService::prepareDetailsView)
                                .toList()))
            );
        } catch (Exception e) {
            log.error("Error while creating CSV file", e);
            actions.add("Failed to create CSV file. Check App insights.");
            throw new RuntimeException("Failed to create CSV file", e);
        }
    }

    /**
     * Sends a summary of the CSV creation and upload check to Slack.
     * @param actions List of action descriptions to include in the summary
     */
    private void sendSlackSummary(List<String> actions) {
        ZonedDateTime nowUk = ZonedDateTime.now(ZoneId.of("Europe/London"));
        String timestamp = nowUk.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder sb = new StringBuilder(
            String.format("*:mag: CSV creation and upload check (%s)*\n", timestamp)
        );
        if (!actions.isEmpty()) {
            sb.append("> ❗ Issue found:\n");
            for (String action : actions) {
                sb.append("• ").append(action).append("\n");
            }
            slackClient.sendSlackMessage(sb.toString());
        }
    }
}
