package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import uk.gov.hmcts.reform.fact.data.api.clients.OnspdClient;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.OnspdRelease;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PostcodeReferenceCsvService {

    private static final CsvSchema OUTPUT_SCHEMA = CsvSchema.builder()
        .addColumn("postcode")
        .addColumn("status")
        .addColumn("is_large_user")
        .addColumn("introduced_yyyymm")
        .addColumn("terminated_yyyymm")
        .addColumn("coordinate_quality")
        .addColumn("has_coordinates")
        .addColumn("latitude")
        .addColumn("longitude")
        .setUseHeader(true)
        .build();

    private final OnspdClient onspdClient;
    private final CsvMapper csvMapper;

    public PostcodeReferenceCsvService(OnspdClient onspdClient) {
        this.onspdClient = onspdClient;
        this.csvMapper = CsvMapper.builder().build();
    }

    /**
     * Writes only large-user and terminated postcodes so the stored file remains small enough for routine imports.
     *
     * @param release ONSPD release to export
     * @param destination temporary output file
     * @return number of records written
     */
    public long createReducedCsv(OnspdRelease release, Path destination) {
        long expectedRecordCount = onspdClient.getFilteredPostcodeCount(release);
        if (expectedRecordCount < 1) {
            throw new PostcodeReferenceImportException("ONSPD query returned no large-user or terminated postcodes");
        }

        long written = 0;
        long lastObjectId = 0;
        try (OutputStream outputStream = Files.newOutputStream(destination);
             SequenceWriter csvWriter = csvMapper
                 .writerFor(new TypeReference<Map<String, Object>>() { })
                 .with(OUTPUT_SCHEMA)
                 .writeValues(outputStream)) {

            while (written < expectedRecordCount) {
                List<JsonNode> records = onspdClient.getPostcodeRecordsAfter(release, lastObjectId);
                if (records.isEmpty()) {
                    throw new PostcodeReferenceImportException(
                        "ONSPD pagination ended after " + written + " of " + expectedRecordCount + " records"
                    );
                }

                for (JsonNode attributes : records) {
                    long objectId = requiredLong(attributes, "ObjectId");
                    if (objectId <= lastObjectId) {
                        throw new PostcodeReferenceImportException(
                            "ONSPD records were not returned in ascending ObjectId order");
                    }
                    csvWriter.write(toOutputRow(attributes));
                    lastObjectId = objectId;
                }
                written += records.size();

                if (written % 100_000 < records.size()) {
                    log.info("Processed {} of {} ONSPD records", written, expectedRecordCount);
                }
            }
        } catch (IOException ex) {
            throw new PostcodeReferenceImportException("Unable to create reduced ONSPD CSV", ex);
        }

        if (written != expectedRecordCount) {
            throw new PostcodeReferenceImportException(
                "ONSPD record count changed during export: expected " + expectedRecordCount + " but wrote " + written
            );
        }
        return written;
    }

    private Map<String, Object> toOutputRow(JsonNode attributes) {
        String postcode = requiredText(attributes, "pcds").trim().replaceAll("\\s+", " ");
        final String introduced = requiredText(attributes, "dointr");
        String terminated = optionalText(attributes, "doterm");
        String userType = requiredText(attributes, "usrtypind");
        final String coordinateQuality = requiredText(attributes, "gridind");
        String latitude = optionalText(attributes, "lat");
        String longitude = optionalText(attributes, "long");

        boolean largeUser = "1".equals(userType);
        boolean terminatedPostcode = terminated != null;
        if (!largeUser && !terminatedPostcode) {
            throw new PostcodeReferenceImportException(
                "ONSPD source returned a row outside the requested filter: " + postcode);
        }

        boolean hasCoordinates = latitude != null && longitude != null;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("postcode", postcode);
        row.put("status", terminatedPostcode ? "terminated" : "current");
        row.put("is_large_user", largeUser);
        row.put("introduced_yyyymm", introduced);
        row.put("terminated_yyyymm", terminated);
        row.put("coordinate_quality", coordinateQuality);
        row.put("has_coordinates", hasCoordinates);
        row.put("latitude", latitude);
        row.put("longitude", longitude);
        return row;
    }

    private String requiredText(JsonNode attributes, String field) {
        String value = optionalText(attributes, field);
        if (value == null) {
            throw new PostcodeReferenceImportException("ONSPD source row is missing required field " + field);
        }
        return value;
    }

    private long requiredLong(JsonNode attributes, String field) {
        JsonNode value = attributes.path(field);
        if (!value.canConvertToLong()) {
            throw new PostcodeReferenceImportException("ONSPD source row is missing required field " + field);
        }
        return value.asLong();
    }

    private String optionalText(JsonNode attributes, String field) {
        JsonNode value = attributes.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
