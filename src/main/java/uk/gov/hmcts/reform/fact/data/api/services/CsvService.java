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

    private final CourtService courtService;
    private final ObjectMapper objectMapper;

    public CsvService(CourtService courtService, ObjectMapper objectMapper) {
        this.courtService = courtService;
        this.objectMapper = objectMapper;
    }

    public String[] getCsvFiles() {
        return new String[] {"courts.csv"};
    }

    public JsonNode convertCourtDataToJson() {
        try {
            return objectMapper.valueToTree(courtService.getAllCourtDetails());
        } catch (IllegalArgumentException ex) {
            throw new JsonConvertException("Error converting court data to JSON", ex);
        }
    }
}
