package uk.gov.hmcts.reform.fact.data.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.JsonConvertException;

/**
 * Service for handling CSV file operations.
 */
@Service
public class CsvService {

    private static final String CSV_CONTAINER_NAME = "csv";
    private static final String COURT_DATA_CSV_FILE_NAME = "courts-and-tribunals-data.csv";

    private final CourtService courtService;
    private final AzureBlobService azureBlobService;
    private final ObjectMapper objectMapper;

    public CsvService(CourtService courtService,
                      AzureBlobService azureBlobService,
                      ObjectMapper objectMapper) {
        this.courtService = courtService;
        this.azureBlobService = azureBlobService;
        this.objectMapper = objectMapper;
    }

    public JsonNode createCsvFiles() {
        JsonNode courtData = convertCourtDataToJson();
        azureBlobService.createCsvFileAndUpload(CSV_CONTAINER_NAME, COURT_DATA_CSV_FILE_NAME, courtData);
        return courtData;
    }

    public JsonNode convertCourtDataToJson() {
        try {
            return objectMapper.valueToTree(courtService.getAllCourtDetails());
        } catch (IllegalArgumentException ex) {
            throw new JsonConvertException("Error converting court data to JSON", ex);
        }
    }
}
