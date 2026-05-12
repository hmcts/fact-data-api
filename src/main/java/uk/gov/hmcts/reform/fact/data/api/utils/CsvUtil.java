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

    public Map<String, Object> flattenCourtNode(JsonNode node) {
        Map<String, Object> flatMap = new LinkedHashMap<>();
        JsonNode courtCode = getFirstArrayItem(node, "courtCodes");
        JsonNode primaryAddress = getFirstArrayItem(node, "courtAddresses");

        flatMap.put("name", node.path("name").asText());
        flatMap.put("lat", readDecimal(node, primaryAddress, "lat"));
        flatMap.put("lon", readDecimal(node, primaryAddress, "lon"));
        flatMap.put("number", readInteger(node, "number"));
        flatMap.put("cci_code", readInteger(courtCode, "countyCourtCode", "cci_code"));
        flatMap.put("magistrate_code", readInteger(courtCode, "magistrateCourtCode", "magistrate_code",
                                                    "magistrate_court_code"));
        flatMap.put("slug", node.path("slug").asText());
        flatMap.put("types", flattenTypes(node));
        flatMap.put("open", readBoolean(node, "open", "displayed", "openOnCath"));
        flatMap.put("dx_number", flattenDxCodes(node));

        flatMap.put("areas_of_law", flattenAreasOfLaw(node.path("courtAreasOfLaw"), node.path("areas_of_law")));
        flatMap.put("addresses", flattenAddresses(node.path("courtAddresses"), node.path("addresses")));

        return flatMap;
    }

    private String flattenAreasOfLaw(JsonNode... candidateNodes) {
        JsonNode courtAreasOfLaw = getFirstArrayCandidate(candidateNodes);
        if (courtAreasOfLaw == null || courtAreasOfLaw.isEmpty()) {
            return "No areas of law available";
        }

        List<String> areas = new ArrayList<>();
        for (JsonNode courtArea : courtAreasOfLaw) {
            JsonNode areaList = courtArea.path("areasOfLaw");
            if (areaList.isArray()) {
                for (JsonNode area : areaList) {
                    String areaDetails = String.format(
                        "Name: %s, External Link: %s, Description: %s, Display Name: %s, Display External Link: %s",
                        safeText(area, "name"),
                        safeText(area, "externalLink", "external_link"),
                        safeText(area, "externalLinkDesc", "external_link_desc"),
                        safeText(area, "displayName", "display_name"),
                        safeText(area, "displayExternalLink", "display_external_link")
                    );
                    areas.add(areaDetails);
                }
            }
        }

        return areas.isEmpty() ? "No areas of law available" : String.join(" | ", areas);
    }

    private String flattenAddresses(JsonNode... candidateNodes) {
        JsonNode addressesNode = getFirstArrayCandidate(candidateNodes);
        if (addressesNode == null) {
            return "No address available";
        }
        if (!addressesNode.isArray() || addressesNode.isEmpty()) {
            return "No address available";
        }

        List<String> addresses = new ArrayList<>();
        for (JsonNode address : addressesNode) {
            String addressDetails = String.format(
                "Town: %s, Postcode: %s, Address: %s, Type: %s, County: %s, %s, Description: %s, EPIM ID: %s",
                safeText(address, "townCity", "town"),
                safeText(address, "postcode"),
                flattenAddressLines(address.path("addressLines"), address.path("address_lines"), address),
                safeText(address, "addressType", "type"),
                safeText(address, "county"),
                flattenFieldsOfLaw(address.path("fieldsOfLaw"), address.path("fields_of_law"), address),
                safeText(address, "description"),
                safeText(address, "epimId", "epim_id")
            );

            addresses.add(addressDetails);
        }

        return String.join(" | ", addresses);
    }

    private String flattenAddressLines(JsonNode... candidateNodes) {
        JsonNode lines = getFirstArrayCandidate(candidateNodes);
        if ((lines == null || !lines.isArray())
            && candidateNodes.length > 0
            && candidateNodes[candidateNodes.length - 1].isObject()) {
            JsonNode address = candidateNodes[candidateNodes.length - 1];
            List<String> implicitLines = new ArrayList<>();
            addIfPresent(implicitLines, safeText(address, "addressLine1"));
            addIfPresent(implicitLines, safeText(address, "addressLine2"));
            if (!implicitLines.isEmpty()) {
                return String.join(", ", implicitLines);
            }
        }

        if (lines == null || !lines.isArray() || lines.isEmpty()) {
            return "No address lines";
        }

        List<String> lineList = new ArrayList<>();
        lines.forEach(line -> lineList.add(line.asText()));
        return String.join(", ", lineList);
    }

    private String flattenFieldsOfLaw(JsonNode... candidateNodes) {
        JsonNode fieldsOfLawNode = getFirstObjectCandidate(candidateNodes);
        if (fieldsOfLawNode == null || !fieldsOfLawNode.isObject()) {
            JsonNode addressNode = candidateNodes.length > 0 ? candidateNodes[candidateNodes.length - 1] : null;
            if (addressNode != null && addressNode.isObject()) {
                List<String> parts = new ArrayList<>();
                addNamesFromArray(parts, addressNode.path("areasOfLaw"), "Areas of Law");
                addNamesFromArray(parts, addressNode.path("courtTypes"), "Courts");
                return parts.isEmpty() ? "N/A" : String.join(", ", parts);
            }
            return "N/A";
        }

        List<String> parts = new ArrayList<>();

        JsonNode areas = fieldsOfLawNode.path("areasOfLaw");
        if (areas.isMissingNode()) {
            areas = fieldsOfLawNode.path("areas_of_law");
        }
        if (areas.isArray() && !areas.isEmpty()) {
            List<String> names = new ArrayList<>();
            areas.forEach(a -> names.add(a.isTextual() ? a.asText() : safeText(a, "name")));
            parts.add("Areas of Law: " + String.join(" | ", names));
        }

        JsonNode courts = fieldsOfLawNode.path("courts");
        if (courts.isMissingNode()) {
            courts = fieldsOfLawNode.path("courtTypes");
        }
        if (courts.isArray() && !courts.isEmpty()) {
            List<String> names = new ArrayList<>();
            courts.forEach(c -> names.add(c.isTextual() ? c.asText() : safeText(c, "name")));
            parts.add("Courts: " + String.join(" | ", names));
        }

        return String.join(", ", parts);
    }

    private String flattenDxCodes(JsonNode node) {
        JsonNode dxCodes = node.path("courtDxCodes");
        if (dxCodes.isArray() && !dxCodes.isEmpty()) {
            List<String> values = new ArrayList<>();
            for (JsonNode dxCode : dxCodes) {
                String value = safeText(dxCode, "dxCode", "dx_number");
                if (!"N/A".equals(value)) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                return String.join(" | ", values);
            }
        }
        return node.path("dx_number").asText();
    }

    private String flattenTypes(JsonNode node) {
        if (node.path("types").isArray()) {
            return stringifyArray(node.path("types"));
        }

        List<String> types = new ArrayList<>();
        JsonNode addresses = node.path("courtAddresses");
        if (addresses.isArray()) {
            for (JsonNode address : addresses) {
                JsonNode courtTypes = address.path("courtTypes");
                if (courtTypes.isArray()) {
                    for (JsonNode courtType : courtTypes) {
                        if (courtType.isTextual()) {
                            types.add(courtType.asText());
                        } else {
                            String name = safeText(courtType, "name");
                            if (!"N/A".equals(name)) {
                                types.add(name);
                            }
                        }
                    }
                }
            }
        }

        if (types.isEmpty()) {
            JsonNode counterServiceOpeningHours = node.path("courtCounterServiceOpeningHours");
            if (counterServiceOpeningHours.isArray()) {
                for (JsonNode openingHour : counterServiceOpeningHours) {
                    JsonNode courtTypes = openingHour.path("courtTypes");
                    if (courtTypes.isArray()) {
                        for (JsonNode courtType : courtTypes) {
                            String name = courtType.isTextual() ? courtType.asText() : safeText(courtType, "name");
                            if (!"N/A".equals(name) && !types.contains(name)) {
                                types.add(name);
                            }
                        }
                    }
                }
            }
        }

        return String.join(" | ", types);
    }

    private JsonNode getFirstArrayItem(JsonNode node, String field) {
        JsonNode array = node.path(field);
        if (array.isArray() && !array.isEmpty()) {
            return array.get(0);
        }
        return null;
    }

    private JsonNode getFirstArrayCandidate(JsonNode... nodes) {
        for (JsonNode candidate : nodes) {
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
        }
        return null;
    }

    private JsonNode getFirstObjectCandidate(JsonNode... nodes) {
        for (JsonNode candidate : nodes) {
            if (candidate != null && candidate.isObject()) {
                return candidate;
            }
        }
        return null;
    }

    private Double readDecimal(JsonNode primary, JsonNode secondary, String field) {
        JsonNode value = primary.path(field);
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (secondary != null) {
            value = secondary.path(field);
            if (value.isNumber()) {
                return value.asDouble();
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isNumber()) {
                return value.asInt();
            }
        }
        return null;
    }

    private Boolean readBoolean(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isBoolean()) {
                return value.asBoolean();
            }
        }
        return false;
    }

    private String stringifyArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        arrayNode.forEach(n -> items.add(n.asText()));
        return String.join(" | ", items);
    }

    private String safeText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return "N/A";
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                return field.asText();
            }
        }
        return "N/A";
    }

    private void addIfPresent(List<String> values, String value) {
        if (value != null && !value.isBlank() && !"N/A".equals(value)) {
            values.add(value);
        }
    }

    private void addNamesFromArray(List<String> parts, JsonNode arrayNode, String label) {
        if (arrayNode.isArray() && !arrayNode.isEmpty()) {
            List<String> names = new ArrayList<>();
            arrayNode.forEach(item -> {
                if (item.isTextual()) {
                    names.add(item.asText());
                } else {
                    String name = safeText(item, "name");
                    if (!"N/A".equals(name)) {
                        names.add(name);
                    }
                }
            });
            if (!names.isEmpty()) {
                parts.add(label + ": " + String.join(" | ", names));
            }
        }
    }
}
