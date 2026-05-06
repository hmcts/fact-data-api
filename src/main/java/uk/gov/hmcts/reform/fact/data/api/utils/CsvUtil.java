package uk.gov.hmcts.reform.fact.data.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.JsonConvertException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting court-related JSON data into a flat CSV format.
 *
 * <p>This class uses the Jackson CSV and JSON libraries to parse and flatten complex, nested JSON structures
 * representing court details into a simplified CSV format. It includes specific logic to handle various fields
 * such as court names, geolocation, types, addresses, and areas of law, transforming them into string
 * representations suitable for CSV output</p>.
 *
 * <p>Key functionality includes:
 * - Flattening nested JSON nodes (like addresses and areas of law) into single string fields.
 * - Building a consistent CSV schema for output.
 * - Safely handling missing or null fields to ensure robust CSV generation</p>.
 *
 * <p>Intended for use in services or tools that need to export structured court data from JSON APIs into a
 * human-readable and spreadsheet-friendly CSV format</p>.
 */
public class CsvUtil {

    private final CsvMapper csvMapper;

    protected CsvUtil(CsvMapper csvMapper) {
        this.csvMapper = csvMapper;
    }

    public CsvUtil() {
        this.csvMapper = new CsvMapper();
        this.csvMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String convertJsonToCsv(JsonNode jsonArrayNode) {
        CsvSchema schema = buildCsvSchema();
        List<Map<String, Object>> flatList = new ArrayList<>();

        for (JsonNode node : jsonArrayNode) {
            flatList.add(flattenCourtNode(node));
        }

        try {
            return csvMapper.writer(schema).writeValueAsString(flatList);
        } catch (RuntimeException | JsonProcessingException ex) {
            throw new JsonConvertException("Failed to convert JSON to CSV: " + ex.getMessage());
        }
    }

    private CsvSchema buildCsvSchema() {
        return CsvSchema.builder()
            .addColumn("name")
            .addColumn("lat")
            .addColumn("lon")
            .addColumn("number")
            .addColumn("cci_code")
            .addColumn("magistrate_code")
            .addColumn("slug")
            .addColumn("types")
            .addColumn("open")
            .addColumn("dx_number")
            .addColumn("areas_of_law")
            .addColumn("addresses")
            .build()
            .withHeader();
    }

    private Map<String, Object> flattenCourtNode(JsonNode node) {
        Map<String, Object> flatMap = new LinkedHashMap<>();

        flatMap.put("name", node.path("name").asText());
        flatMap.put("lat", node.path("lat").asDouble());
        flatMap.put("lon", node.path("lon").asDouble());
        flatMap.put("number", node.path("number").asInt());
        flatMap.put("cci_code", node.path("cci_code").asInt());
        flatMap.put("magistrate_code", node.path("magistrate_code").asInt());
        flatMap.put("slug", node.path("slug").asText());
        flatMap.put("types", stringifyArray(node.path("types")));
        flatMap.put("open", node.path("displayed").asBoolean());
        flatMap.put("dx_number", node.path("dx_number").asText());

        flatMap.put("areas_of_law", flattenAreasOfLaw(node.path("areas_of_law")));
        flatMap.put("addresses", flattenAddresses(node.path("addresses")));

        return flatMap;
    }

    private String flattenAreasOfLaw(JsonNode areasOfLawNode) {
        if (!areasOfLawNode.isArray() || areasOfLawNode.isEmpty()) {
            return "No areas of law available";
        }

        List<String> areas = new ArrayList<>();
        for (JsonNode area : areasOfLawNode) {
            String areaDetails = String.format(
                "Name: %s, External Link: %s, Description: %s, Display Name: %s, Display External Link: %s",
                safeText(area, "name"),
                safeText(area, "external_link"),
                safeText(area, "external_link_desc"),
                safeText(area, "display_name"),
                safeText(area, "display_external_link")
            );
            areas.add(areaDetails);
        }

        return String.join(" | ", areas);
    }

    private String flattenAddresses(JsonNode addressesNode) {
        if (!addressesNode.isArray() || addressesNode.isEmpty()) {
            return "No address available";
        }

        List<String> addresses = new ArrayList<>();
        for (JsonNode address : addressesNode) {
            String addressDetails = String.format(
                "Town: %s, Postcode: %s, Address: %s, Type: %s, County: %s, %s, Description: %s, EPIM ID: %s",
                safeText(address, "town"),
                safeText(address, "postcode"),
                flattenAddressLines(address.path("address_lines")),
                safeText(address, "type"),
                safeText(address, "county"),
                flattenFieldsOfLaw(address.path("fields_of_law")),
                safeText(address, "description"),
                safeText(address, "epim_id")
            );

            addresses.add(addressDetails);
        }

        return String.join(" | ", addresses);
    }

    private String flattenAddressLines(JsonNode lines) {
        if (!lines.isArray() || lines.isEmpty()) {
            return "No address lines";
        }

        List<String> lineList = new ArrayList<>();
        lines.forEach(line -> lineList.add(line.asText()));
        return String.join(", ", lineList);
    }

    private String flattenFieldsOfLaw(JsonNode fieldsOfLawNode) {
        if (!fieldsOfLawNode.isObject()) {
            return "N/A";
        }

        List<String> parts = new ArrayList<>();

        JsonNode areas = fieldsOfLawNode.path("areas_of_law");
        if (areas.isArray() && !areas.isEmpty()) {
            List<String> names = new ArrayList<>();
            areas.forEach(a -> names.add(a.asText()));
            parts.add("Areas of Law: " + String.join(" | ", names));
        }

        JsonNode courts = fieldsOfLawNode.path("courts");
        if (courts.isArray() && !courts.isEmpty()) {
            List<String> names = new ArrayList<>();
            courts.forEach(c -> names.add(c.asText()));
            parts.add("Courts: " + String.join(" | ", names));
        }

        return String.join(", ", parts);
    }

    private String stringifyArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        arrayNode.forEach(n -> items.add(n.asText()));
        return String.join(" | ", items);
    }

    private String safeText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? "N/A" : field.asText();
    }
}
