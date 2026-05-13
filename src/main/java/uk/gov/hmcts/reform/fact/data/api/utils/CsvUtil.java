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
    private static final String NAME = "name";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String SLUG = "slug";
    private static final String TYPES = "types";
    private static final String CCI_CODE = "cci_code";
    private static final String MAGISTRATE_CODE = "magistrate_code";
    private static final String AREAS_OF_LAW = "areas_of_law";
    private static final String AREAS_OF_LAW_PATH = "areasOfLaw";
    private static final String ADDRESSES = "addresses";
    private static final String COURT_ADDRESSES = "courtAddresses";
    private static final String COURT_TYPES = "courtTypes";
    private static final String DX_NUMBER = "dx_number";
    private static final String NOT_AVAILABLE = "N/A";
    private static final String PIPE_SEPARATOR = " | ";
    private static final String NO_AREAS_OF_LAW_AVAILABLE = "No areas of law available";
    private static final String NO_ADDRESS_AVAILABLE = "No address available";
    private static final String AREAS_OF_LAW_LABEL = "Areas of Law";
    private static final String COURTS_LABEL = "Courts";

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
            .addColumn(NAME)
            .addColumn(LAT)
            .addColumn(LON)
            .addColumn("number")
            .addColumn(CCI_CODE)
            .addColumn(MAGISTRATE_CODE)
            .addColumn(SLUG)
            .addColumn(TYPES)
            .addColumn("open")
            .addColumn(DX_NUMBER)
            .addColumn(AREAS_OF_LAW)
            .addColumn(ADDRESSES)
            .build()
            .withHeader();
    }

    public Map<String, Object> flattenCourtNode(JsonNode node) {
        Map<String, Object> flatMap = new LinkedHashMap<>();
        JsonNode courtCode = getFirstArrayItem(node, "courtCodes");
        JsonNode primaryAddress = getFirstArrayItem(node, COURT_ADDRESSES);

        flatMap.put(NAME, node.path(NAME).asText());
        flatMap.put(LAT, readDecimal(node, primaryAddress, LAT));
        flatMap.put(LON, readDecimal(node, primaryAddress, LON));
        flatMap.put("number", readInteger(courtCode, "crownCourtCode", "crown_court_code"));
        flatMap.put(CCI_CODE, readInteger(courtCode, "countyCourtCode", "county_court_code", CCI_CODE));
        flatMap.put(MAGISTRATE_CODE, readInteger(
            courtCode, "magistrateCourtCode", "magistrate_court_code", MAGISTRATE_CODE));
        flatMap.put(SLUG, node.path(SLUG).asText());
        flatMap.put(TYPES, flattenTypes(node));
        flatMap.put("open", readBoolean(node, "open", "displayed", "openOnCath"));
        flatMap.put(DX_NUMBER, flattenDxCodes(node));

        flatMap.put(AREAS_OF_LAW, flattenAreasOfLaw(node.path("courtAreasOfLaw"), node.path(AREAS_OF_LAW)));
        flatMap.put(ADDRESSES, flattenAddresses(node.path(COURT_ADDRESSES), node.path(ADDRESSES)));

        return flatMap;
    }

    private String flattenAreasOfLaw(JsonNode... candidateNodes) {
        JsonNode courtAreasOfLaw = getFirstArrayCandidate(candidateNodes);
        if (courtAreasOfLaw == null || courtAreasOfLaw.isEmpty()) {
            return NO_AREAS_OF_LAW_AVAILABLE;
        }

        List<String> areas = new ArrayList<>();
        for (JsonNode courtArea : courtAreasOfLaw) {
            JsonNode areaList = courtArea.path(AREAS_OF_LAW_PATH);
            if (areaList.isArray()) {
                for (JsonNode area : areaList) {
                    String areaDetails = String.format(
                        "Name: %s, External Link: %s, Description: %s, Display Name: %s, Display External Link: %s",
                        safeText(area, NAME),
                        safeText(area, "externalLink", "external_link"),
                        safeText(area, "externalLinkDesc", "external_link_desc"),
                        safeText(area, "displayName", "display_name"),
                        safeText(area, "displayExternalLink", "display_external_link")
                    );
                    areas.add(areaDetails);
                }
            }
        }

        return areas.isEmpty() ? NO_AREAS_OF_LAW_AVAILABLE : String.join(PIPE_SEPARATOR, areas);
    }

    private String flattenAddresses(JsonNode... candidateNodes) {
        JsonNode addressesNode = getFirstArrayCandidate(candidateNodes);
        if (addressesNode == null) {
            return NO_ADDRESS_AVAILABLE;
        }
        if (!addressesNode.isArray() || addressesNode.isEmpty()) {
            return NO_ADDRESS_AVAILABLE;
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

        return String.join(PIPE_SEPARATOR, addresses);
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
            return flattenFieldsOfLawFromAddress(candidateNodes);
        }

        List<String> parts = new ArrayList<>();
        addFieldsOfLawAreas(parts, fieldsOfLawNode);
        addFieldsOfLawCourts(parts, fieldsOfLawNode);

        return parts.isEmpty() ? NOT_AVAILABLE : String.join(", ", parts);
    }

    private String flattenFieldsOfLawFromAddress(JsonNode... candidateNodes) {
        JsonNode addressNode = candidateNodes.length > 0 ? candidateNodes[candidateNodes.length - 1] : null;
        if (addressNode != null && addressNode.isObject()) {
            List<String> parts = new ArrayList<>();
            addNamesFromArray(parts, addressNode.path(AREAS_OF_LAW_PATH), AREAS_OF_LAW_LABEL);
            addNamesFromArray(parts, addressNode.path(COURT_TYPES), COURTS_LABEL);
            return parts.isEmpty() ? NOT_AVAILABLE : String.join(", ", parts);
        }
        return NOT_AVAILABLE;
    }

    private void addFieldsOfLawAreas(List<String> parts, JsonNode fieldsOfLawNode) {
        JsonNode areas = fieldsOfLawNode.path(AREAS_OF_LAW_PATH);
        if (areas.isMissingNode()) {
            areas = fieldsOfLawNode.path(AREAS_OF_LAW);
        }
        if (areas.isArray() && !areas.isEmpty()) {
            List<String> names = new ArrayList<>();
            areas.forEach(a -> names.add(a.isTextual() ? a.asText() : safeText(a, NAME)));
            parts.add(AREAS_OF_LAW_LABEL + ": " + String.join(PIPE_SEPARATOR, names));
        }
    }

    private void addFieldsOfLawCourts(List<String> parts, JsonNode fieldsOfLawNode) {
        JsonNode courts = fieldsOfLawNode.path("courts");
        if (courts.isMissingNode()) {
            courts = fieldsOfLawNode.path(COURT_TYPES);
        }
        if (courts.isArray() && !courts.isEmpty()) {
            List<String> names = new ArrayList<>();
            courts.forEach(c -> names.add(c.isTextual() ? c.asText() : safeText(c, NAME)));
            parts.add(COURTS_LABEL + ": " + String.join(PIPE_SEPARATOR, names));
        }
    }

    private String flattenDxCodes(JsonNode node) {
        JsonNode dxCodes = node.path("courtDxCodes");
        if (dxCodes.isArray() && !dxCodes.isEmpty()) {
            List<String> values = new ArrayList<>();
            for (JsonNode dxCode : dxCodes) {
                String value = safeText(dxCode, "dxCode", DX_NUMBER);
                if (!NOT_AVAILABLE.equals(value)) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                return String.join(PIPE_SEPARATOR, values);
            }
        }
        return node.path(DX_NUMBER).asText();
    }

    private String flattenTypes(JsonNode node) {
        if (node.path(TYPES).isArray()) {
            return stringifyArray(node.path(TYPES));
        }

        List<String> types = new ArrayList<>();
        collectTypesFromAddresses(node.path(COURT_ADDRESSES), types);

        if (types.isEmpty()) {
            collectTypesFromCounterService(node.path("courtCounterServiceOpeningHours"), types);
        }

        return String.join(PIPE_SEPARATOR, types);
    }

    private void collectTypesFromAddresses(JsonNode addresses, List<String> types) {
        if (addresses.isArray()) {
            for (JsonNode address : addresses) {
                JsonNode courtTypes = address.path(COURT_TYPES);
                if (courtTypes.isArray()) {
                    for (JsonNode courtType : courtTypes) {
                        addTypeNameToList(courtType, types);
                    }
                }
            }
        }
    }

    private void collectTypesFromCounterService(JsonNode counterServiceOpeningHours, List<String> types) {
        if (counterServiceOpeningHours.isArray()) {
            for (JsonNode openingHour : counterServiceOpeningHours) {
                JsonNode courtTypes = openingHour.path(COURT_TYPES);
                if (courtTypes.isArray()) {
                    for (JsonNode courtType : courtTypes) {
                        addTypeNameToList(courtType, types);
                    }
                }
            }
        }
    }

    private void addTypeNameToList(JsonNode courtType, List<String> types) {
        String name = courtType.isTextual() ? courtType.asText() : safeText(courtType, NAME);
        if (!NOT_AVAILABLE.equals(name) && !types.contains(name)) {
            types.add(name);
        }
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
        return String.join(PIPE_SEPARATOR, items);
    }

    private String safeText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return NOT_AVAILABLE;
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                return field.asText();
            }
        }
        return NOT_AVAILABLE;
    }

    private void addIfPresent(List<String> values, String value) {
        if (value != null && !value.isBlank() && !NOT_AVAILABLE.equals(value)) {
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
                    String name = safeText(item, NAME);
                    if (!NOT_AVAILABLE.equals(name)) {
                        names.add(name);
                    }
                }
            });
            if (!names.isEmpty()) {
                parts.add(label + ": " + String.join(PIPE_SEPARATOR, names));
            }
        }
    }
}
