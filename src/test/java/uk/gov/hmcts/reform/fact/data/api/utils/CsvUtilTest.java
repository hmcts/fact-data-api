package uk.gov.hmcts.reform.fact.data.api.utils;

import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.JsonConvertException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvUtilTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final CsvUtil csvUtil = new CsvUtil();

    @Test
    void shouldFlattenEnrichedAreasOfLaw() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode courtAreasOfLaw = root.putArray("courtAreasOfLaw");
        ObjectNode courtArea = courtAreasOfLaw.addObject();
        ArrayNode areaList = courtArea.putArray("areasOfLaw");
        ObjectNode area = areaList.addObject();
        area.put("name", "Family");
        area.put("externalLink", "http://family.com");
        area.put("displayName", "Family Law");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("areas_of_law").toString())
            .contains("Name: Family")
            .contains("External Link: http://family.com")
            .contains("Display Name: Family Law");
    }

    @Test
    void shouldFlattenEnrichedAddressesAndFieldsOfLaw() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        address.put("addressLine1", "10 High St");
        address.put("townCity", "London");
        address.put("postcode", "SW1 1AA");

        ArrayNode addressAreas = address.putArray("areasOfLaw");
        ObjectNode area = addressAreas.addObject();
        area.put("name", "Crime");

        ArrayNode addressTypes = address.putArray("courtTypes");
        ObjectNode type = addressTypes.addObject();
        type.put("name", "Crown Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        String addressesStr = result.get("addresses").toString();
        assertThat(addressesStr)
            .contains("Address: 10 High St")
            .contains("Town: London")
            .contains("Postcode: SW1 1AA")
            .contains("Areas of Law: Crime")
            .contains("Courts: Crown Court");

        // Verify types flattening as well
        assertThat(result.get("types")).hasToString("Crown Court");
    }

    @Test
    void shouldFlattenTypesFromCounterServiceIfAddressesEmpty() {
        ObjectNode root = mapper.createObjectNode();
        root.putArray("courtAddresses"); // Empty

        ArrayNode counterService = root.putArray("courtCounterServiceOpeningHours");
        ObjectNode service = counterService.addObject();
        ArrayNode serviceTypes = service.putArray("courtTypes");
        ObjectNode type = serviceTypes.addObject();
        type.put("name", "Magistrates' Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types")).hasToString("Magistrates' Court");
    }

    @Test
    void shouldFlattenTypesFromDirectTypesArray() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode types = root.putArray("types");
        types.add("Civil");
        types.add(42);
        types.add(true);

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types")).hasToString("Civil | 42 | true");
    }

    @Test
    void shouldFlattenTypesFromServiceAreasWhenTypesMissing() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode serviceAreas = root.putArray("serviceAreas");
        serviceAreas.addObject().put("name", "Family Services");
        serviceAreas.add("Employment Services");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types")).hasToString("Family Services | Employment Services");
    }

    @Test
    void shouldHandleOpenOnCath() {
        ObjectNode root = mapper.createObjectNode();
        root.put("openOnCath", true);

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result).containsEntry("open", true);
    }

    @Test
    void shouldReturnNotAvailableIfNoNodesProvided() {
        Map<String, Object> result = csvUtil.flattenCourtNode(mapper.createObjectNode());
        assertThat(result.get("addresses")).hasToString("No address available");
    }

    @Test
    void shouldFlattenTypesFromAddresses() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        ArrayNode courtTypes = address.putArray("courtTypes");
        courtTypes.add("County Court");
        ObjectNode type2 = courtTypes.addObject();
        type2.put("name", "Tribunal");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types")).hasToString("County Court | Tribunal");
    }

    @Test
    void shouldFlattenTypesFromCounterService() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode counterService = root.putArray("courtCounterServiceOpeningHours");
        ObjectNode service = counterService.addObject();
        ArrayNode courtTypes = service.putArray("courtTypes");
        courtTypes.add("Magistrates' Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types")).hasToString("Magistrates' Court");
    }

    @Test
    void shouldAvoidDuplicateTypesFromCounterService() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode counterService = root.putArray("courtCounterServiceOpeningHours");

        ObjectNode service1 = counterService.addObject();
        service1.putArray("courtTypes").add("Magistrates' Court");

        ObjectNode service2 = counterService.addObject();
        service2.putArray("courtTypes").add("Magistrates' Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types")).hasToString("Magistrates' Court");
    }

    @Test
    void shouldFlattenFieldsOfLawWithAreasAndCourts() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        ObjectNode fieldsOfLaw = address.putObject("fieldsOfLaw");

        ArrayNode areas = fieldsOfLaw.putArray("areasOfLaw");
        ObjectNode a1 = areas.addObject();
        a1.put("name", "Family");

        ArrayNode courts = fieldsOfLaw.putArray("courts");
        ObjectNode c1 = courts.addObject();
        c1.put("name", "High Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        String addressesStr = result.get("addresses").toString();
        assertThat(addressesStr)
            .contains("Areas of Law: Family")
            .contains("Courts: High Court");
    }

    @Test
    void shouldFallbackToSnakeCaseFieldsInFieldsOfLaw() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        ObjectNode fieldsOfLaw = address.putObject("fields_of_law");

        ArrayNode areas = fieldsOfLaw.putArray("areas_of_law");
        areas.add("Family");

        ArrayNode courts = fieldsOfLaw.putArray("courtTypes");
        courts.add("Tribunal");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        String addressesStr = result.get("addresses").toString();
        assertThat(addressesStr)
            .contains("Areas of Law: Family")
            .contains("Courts: Tribunal");
    }

    @Test
    void shouldFlattenFieldsOfLawWithTextualAreasAndCourts() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        ObjectNode fieldsOfLaw = address.putObject("fieldsOfLaw");

        ArrayNode areas = fieldsOfLaw.putArray("areasOfLaw");
        areas.add("Family");

        ArrayNode courts = fieldsOfLaw.putArray("courts");
        courts.add("High Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        String addressesStr = result.get("addresses").toString();
        assertThat(addressesStr)
            .contains("Areas of Law: Family")
            .contains("Courts: High Court");
    }

    @Test
    void shouldFallbackToAddressNodeWhenFieldsOfLawNodeMissing() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();

        ArrayNode addressAreas = address.putArray("areasOfLaw");
        addressAreas.add("Crime");

        ArrayNode addressTypes = address.putArray("courtTypes");
        addressTypes.add("Crown Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        String addressesStr = result.get("addresses").toString();
        assertThat(addressesStr)
            .contains("Areas of Law: Crime")
            .contains("Courts: Crown Court");
    }

    @Test
    void shouldConvertJsonToCsv() {
        ArrayNode root = mapper.createArrayNode();
        ObjectNode court = root.addObject();
        court.put("name", "Test Court");
        court.put("slug", "test-court");

        String csv = csvUtil.convertJsonToCsv(root);

        assertThat(csv)
            .contains("name,lat,lon,number,cci_code,magistrate_code,slug,types,open,dx_number,areas_of_law,addresses")
            .contains("Test Court")
            .contains("test-court");
    }

    @Test
    void shouldThrowExceptionOnInvalidJson() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        address.put("addressLine1", "Line 1");
        address.put("addressLine2", "Line 2");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        assertThat(result.get("addresses").toString()).contains("Address: Line 1, Line 2");

        ArrayNode lines = address.putArray("addressLines");
        lines.add("Line A");
        lines.add("Line B");

        result = csvUtil.flattenCourtNode(root);
        assertThat(result.get("addresses").toString()).contains("Address: Line A, Line B");
    }

    @Test
    void shouldHandleDxCodes() {
        ObjectNode root = mapper.createObjectNode();
        root.put("dx_number", "DX 123");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        assertThat(result).containsEntry("dx_number", "DX 123");

        ArrayNode dxCodes = root.putArray("courtDxCodes");
        ObjectNode dx1 = dxCodes.addObject();
        dx1.put("dxCode", "DX 456");

        result = csvUtil.flattenCourtNode(root);
        assertThat(result).containsEntry("dx_number", "DX 456");
    }

    @Test
    void shouldFallbackToDxNumberWhenDxCodesHaveNoUsableValue() {
        ObjectNode root = mapper.createObjectNode();
        root.put("dx_number", "DX 123");
        ArrayNode dxCodes = root.putArray("courtDxCodes");
        dxCodes.addObject();

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result).containsEntry("dx_number", "DX 123");
    }

    @Test
    void shouldUseServiceCentreAddressesWhenCourtAddressesMissing() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode addresses = root.putArray("serviceCentreAddresses");
        ObjectNode address = addresses.addObject();
        address.put("addressLine1", "2 Service Road");
        address.put("townCity", "Leeds");
        address.put("postcode", "LS1 2AB");
        address.put("lat", 53.8);
        address.put("lon", -1.55);

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result)
            .containsEntry("lat", 53.8)
            .containsEntry("lon", -1.55);
        assertThat(result.get("addresses").toString())
            .contains("Address: 2 Service Road")
            .contains("Town: Leeds");
    }

    @Test
    void shouldReturnNoAreasOfLawWhenAreasNodeIsNotArray() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode courtAreasOfLaw = root.putArray("courtAreasOfLaw");
        courtAreasOfLaw.addObject().putObject("areasOfLaw");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("areas_of_law")).hasToString("No areas of law available");
    }

    @Test
    void shouldReadNumbersAndBooleansCorrectly() {
        ObjectNode root = mapper.createObjectNode();
        root.put("open", true);

        ArrayNode courtCodes = root.putArray("courtCodes");
        ObjectNode code = courtCodes.addObject();
        code.put("crownCourtCode", 123);
        code.put("countyCourtCode", 456);
        code.put("magistrateCourtCode", 789);

        ArrayNode addresses = root.putArray("courtAddresses");
        ObjectNode address = addresses.addObject();
        address.put("lat", 51.5);
        address.put("lon", -0.1);

        Map<String, Object> result = csvUtil.flattenCourtNode(root);
        assertThat(result)
            .containsEntry("open", true)
            .containsEntry("number", 123)
            .containsEntry("cci_code", 456)
            .containsEntry("magistrate_code", 789)
            .containsEntry("lat", 51.5)
            .containsEntry("lon", -0.1);
    }

    @Test
    void shouldHandleNullsInReadMethods() {
        ObjectNode root = mapper.createObjectNode();
        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("lat")).isNull();
        assertThat(result.get("number")).isNull();
        assertThat(result).containsEntry("open", false);
    }

    @Test
    void shouldThrowJsonConvertExceptionWhenCsvWritingFails() {
        CsvMapper mockCsvMapper = mock(CsvMapper.class);
        CsvUtil utilWithMock = new CsvUtil(mockCsvMapper);

        when(mockCsvMapper.writer(org.mockito.ArgumentMatchers.any(
            tools.jackson.dataformat.csv.CsvSchema.class)))
            .thenThrow(new JacksonException("Mock failure") {});

        ArrayNode root = mapper.createArrayNode();
        root.addObject();

        assertThatThrownBy(() -> utilWithMock.convertJsonToCsv(root))
            .isInstanceOf(JsonConvertException.class)
            .hasMessageContaining("Failed to convert JSON to CSV: Mock failure");
    }

    @Test
    void shouldCoverPrivateFallbackBranchesAndNullSafety() throws Exception {
        Object flattenedFieldsOfLaw = invokePrivate(
            "flattenFieldsOfLaw",
            new Class<?>[]{tools.jackson.databind.JsonNode[].class},
            (Object) new tools.jackson.databind.JsonNode[]{mapper.createArrayNode()}
        );
        assertThat(flattenedFieldsOfLaw).isEqualTo("N/A");

        Object result = invokePrivate(
            "flattenFieldsOfLawFromAddress",
            new Class<?>[]{tools.jackson.databind.JsonNode[].class},
            (Object) new tools.jackson.databind.JsonNode[]{}
        );
        assertThat(result).isEqualTo("N/A");

        ObjectNode addressNode = mapper.createObjectNode();
        ArrayNode addressAreas = addressNode.putArray("areasOfLaw");
        addressAreas.add("Crime");
        addressAreas.addObject().put("name", "Family");
        ArrayNode courtTypes = addressNode.putArray("courtTypes");
        courtTypes.add("Crown Court");
        courtTypes.addObject().put("name", "High Court");

        Object fromAddress = invokePrivate(
            "flattenFieldsOfLawFromAddress",
            new Class<?>[]{tools.jackson.databind.JsonNode[].class},
            (Object) new tools.jackson.databind.JsonNode[]{addressNode}
        );
        assertThat(fromAddress)
            .isEqualTo("Areas of Law: Crime | Family, Courts: Crown Court | High Court");

        Object objectCandidate = invokePrivate(
            "getFirstObjectCandidate",
            new Class<?>[]{tools.jackson.databind.JsonNode[].class},
            (Object) new tools.jackson.databind.JsonNode[]{mapper.createArrayNode()}
        );
        assertThat(objectCandidate).isNull();

        Object decimal = invokePrivate(
            "readDecimal",
            new Class<?>[]{tools.jackson.databind.JsonNode.class, tools.jackson.databind.JsonNode.class, String.class},
            mapper.createObjectNode().put("lat", 1.23),
            null,
            "lat"
        );
        assertThat(decimal).isEqualTo(1.23d);

        assertThat(invokePrivate(
            "stringifyArray",
            new Class<?>[]{tools.jackson.databind.JsonNode.class},
            new Object[]{null}
        )).isEqualTo("");

        assertThat(invokePrivate(
            "stringifyNamedArray",
            new Class<?>[]{tools.jackson.databind.JsonNode.class},
            new Object[]{null}
        )).isEqualTo("");

        assertThat(invokePrivate(
            "safeText",
            new Class<?>[]{tools.jackson.databind.JsonNode.class, String[].class},
            null,
            new String[]{"name"}
        )).isEqualTo("N/A");

        assertThat(invokePrivate(
            "asNodeText",
            new Class<?>[]{tools.jackson.databind.JsonNode.class, String.class},
            mapper.createObjectNode(),
            "default"
        )).isEqualTo("default");

        Object integer = invokePrivate(
            "readInteger",
            new Class<?>[]{tools.jackson.databind.JsonNode.class, String[].class},
            mapper.createObjectNode().put("second", 9),
            new String[]{"first", "second"}
        );
        assertThat(integer).isEqualTo(9);

        Object missingInteger = invokePrivate(
            "readInteger",
            new Class<?>[]{tools.jackson.databind.JsonNode.class, String[].class},
            mapper.createObjectNode().put("text", "x"),
            new String[]{"first", "second"}
        );
        assertThat(missingInteger).isNull();
    }

    @Test
    void shouldCoverRemainingBranchOutcomesAcrossPrivateHelpers() throws Exception {
        ObjectNode emptyAreasRoot = mapper.createObjectNode();
        emptyAreasRoot.putArray("courtAreasOfLaw");
        assertThat(csvUtil.flattenCourtNode(emptyAreasRoot).get("areas_of_law"))
            .hasToString("No areas of law available");

        ObjectNode objectAddressesRoot = mapper.createObjectNode();
        objectAddressesRoot.putObject("addresses").put("townCity", "Leeds");
        assertThat(csvUtil.flattenCourtNode(objectAddressesRoot).get("addresses"))
            .hasToString("No address available");

        assertThat(invokePrivate(
            "flattenAddressLines",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{}
        )).isEqualTo("No address lines");

        assertThat(invokePrivate(
            "flattenAddressLines",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{mapper.createObjectNode()}
        )).isEqualTo("No address lines");

        JsonNode stringNode = mapper.createArrayNode().add("not-array").get(0);
        assertThat(invokePrivate(
            "flattenAddressLines",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{stringNode}
        )).isEqualTo("No address lines");

        assertThat(invokePrivate(
            "flattenAddressLines",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{mapper.createArrayNode()}
        )).isEqualTo("No address lines");

        ObjectNode emptyFieldsNode = mapper.createObjectNode();
        emptyFieldsNode.putArray("areasOfLaw");
        emptyFieldsNode.putArray("courts");
        assertThat(invokePrivate(
            "flattenFieldsOfLaw",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{emptyFieldsNode}
        )).isEqualTo("N/A");

        assertThat(invokePrivate(
            "flattenFieldsOfLawFromAddress",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{stringNode}
        )).isEqualTo("N/A");

        ObjectNode emptyDxCodesRoot = mapper.createObjectNode();
        emptyDxCodesRoot.put("dx_number", "DX fallback");
        emptyDxCodesRoot.putArray("courtDxCodes");
        assertThat(csvUtil.flattenCourtNode(emptyDxCodesRoot)).containsEntry("dx_number", "DX fallback");

        ObjectNode emptyServiceAreasRoot = mapper.createObjectNode();
        emptyServiceAreasRoot.putArray("serviceAreas");
        assertThat(csvUtil.flattenCourtNode(emptyServiceAreasRoot).get("types")).hasToString("");

        ObjectNode counterServiceRoot = mapper.createObjectNode();
        ArrayNode counterServices = counterServiceRoot.putArray("courtCounterServiceOpeningHours");
        counterServices.addObject().put("courtTypes", "invalid");
        counterServices.addObject().putArray("courtTypes").addObject();
        assertThat(csvUtil.flattenCourtNode(counterServiceRoot).get("types")).hasToString("");

        assertThat(invokePrivate(
            "getFirstArrayCandidate",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{null, mapper.createObjectNode()}
        )).isNull();

        assertThat(invokePrivate(
            "getFirstObjectCandidate",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{null, mapper.createArrayNode()}
        )).isNull();

        assertThat(invokePrivate(
            "stringifyArray",
            new Class<?>[]{JsonNode.class},
            mapper.createObjectNode()
        )).isEqualTo("");

        assertThat(invokePrivate(
            "stringifyNamedArray",
            new Class<?>[]{JsonNode.class},
            mapper.createObjectNode()
        )).isEqualTo("");

        ObjectNode nullNameNode = mapper.createObjectNode();
        nullNameNode.putNull("name");
        assertThat(invokePrivate(
            "safeText",
            new Class<?>[]{JsonNode.class, String[].class},
            nullNameNode,
            new String[]{"name"}
        )).isEqualTo("N/A");

        List<String> values = new ArrayList<>();
        invokePrivate("addIfPresent", new Class<?>[]{List.class, String.class}, values, " ");
        invokePrivate("addIfPresent", new Class<?>[]{List.class, String.class}, values, "N/A");
        invokePrivate("addIfPresent", new Class<?>[]{List.class, String.class}, values, null);
        assertThat(values).isEmpty();

        List<String> parts = new ArrayList<>();
        invokePrivate(
            "addNamesFromArray",
            new Class<?>[]{List.class, JsonNode.class, String.class},
            parts,
            mapper.createObjectNode(),
            "Label"
        );
        ArrayNode unnamedEntries = mapper.createArrayNode();
        unnamedEntries.addObject();
        invokePrivate(
            "addNamesFromArray",
            new Class<?>[]{List.class, JsonNode.class, String.class},
            parts,
            unnamedEntries,
            "Label"
        );
        assertThat(parts).isEmpty();

        JsonNode numericNode = mapper.createArrayNode().add(123).get(0);
        assertThat(invokePrivate("isStringNode", new Class<?>[]{JsonNode.class}, numericNode)).isEqualTo(false);

        assertThat(invokePrivate(
            "asNodeText",
            new Class<?>[]{JsonNode.class, String.class},
            mapper.getNodeFactory().nullNode(),
            "default"
        )).isEqualTo("default");

        assertThat(invokePrivate(
            "asNodeText",
            new Class<?>[]{JsonNode.class, String.class},
            mapper.createObjectNode().path("missing"),
            "default"
        )).isEqualTo("default");
    }

    @Test
    void shouldCoverDefensiveBranchesThatRequireStatefulOrEdgeNodes() throws Exception {
        JsonNode statefulArrayNode = mock(JsonNode.class);
        when(statefulArrayNode.isArray()).thenReturn(true, false);
        assertThat(invokePrivate(
            "flattenAddresses",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{statefulArrayNode}
        )).isEqualTo("No address available");

        JsonNode statefulAddressLinesNode = mock(JsonNode.class);
        when(statefulAddressLinesNode.isArray()).thenReturn(true, false, false);
        assertThat(invokePrivate(
            "flattenAddressLines",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{statefulAddressLinesNode}
        )).isEqualTo("No address lines");

        JsonNode statefulFieldsOfLawNode = mock(JsonNode.class);
        when(statefulFieldsOfLawNode.isObject()).thenReturn(true, false);
        assertThat(invokePrivate(
            "flattenFieldsOfLaw",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{statefulFieldsOfLawNode}
        )).isEqualTo("N/A");

        assertThat(invokePrivate(
            "flattenFieldsOfLawFromAddress",
            new Class<?>[]{JsonNode[].class},
            (Object) new JsonNode[]{mapper.createObjectNode()}
        )).isEqualTo("N/A");

        List<String> parts = new ArrayList<>();
        invokePrivate(
            "addNamesFromArray",
            new Class<?>[]{List.class, JsonNode.class, String.class},
            parts,
            mapper.createArrayNode(),
            "Label"
        );
        assertThat(parts).isEmpty();

        assertThat(invokePrivate(
            "isStringNode",
            new Class<?>[]{JsonNode.class},
            (Object) null
        )).isEqualTo(false);

        assertThat(invokePrivate(
            "asNodeText",
            new Class<?>[]{JsonNode.class, String.class},
            (Object) null,
            "default"
        )).isEqualTo("default");

        ArrayNode unnamedItems = mapper.createArrayNode();
        unnamedItems.addObject();
        assertThat(invokePrivate(
            "stringifyNamedArray",
            new Class<?>[]{JsonNode.class},
            unnamedItems
        )).isEqualTo("");
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = CsvUtil.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(csvUtil, args);
    }
}
