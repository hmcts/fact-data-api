package uk.gov.hmcts.reform.fact.data.api.utils;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.JsonConvertException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvUtilTest {

    private final ObjectMapper mapper = new ObjectMapper();
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
        assertThat(addressesStr).contains("Address: 10 High St");
        assertThat(addressesStr).contains("Town: London");
        assertThat(addressesStr).contains("Postcode: SW1 1AA");
        assertThat(addressesStr).contains("Areas of Law: Crime");
        assertThat(addressesStr).contains("Courts: Crown Court");

        // Verify types flattening as well
        assertThat(result.get("types").toString()).isEqualTo("Crown Court");
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

        assertThat(result.get("types").toString()).isEqualTo("Magistrates' Court");
    }

    @Test
    void shouldHandleOpenOnCath() {
        ObjectNode root = mapper.createObjectNode();
        root.put("openOnCath", true);

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("open")).isEqualTo(true);
    }

    @Test
    void shouldReturnNotAvailableIfNoNodesProvided() {
        Map<String, Object> result = csvUtil.flattenCourtNode(mapper.createObjectNode());
        assertThat(result.get("addresses").toString()).isEqualTo("No address available");
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

        assertThat(result.get("types").toString()).isEqualTo("County Court | Tribunal");
    }

    @Test
    void shouldFlattenTypesFromCounterService() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode counterService = root.putArray("courtCounterServiceOpeningHours");
        ObjectNode service = counterService.addObject();
        ArrayNode courtTypes = service.putArray("courtTypes");
        courtTypes.add("Magistrates' Court");

        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("types").toString()).isEqualTo("Magistrates' Court");
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

        assertThat(result.get("types").toString()).isEqualTo("Magistrates' Court");
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
        assertThat(addressesStr).contains("Areas of Law: Family");
        assertThat(addressesStr).contains("Courts: High Court");
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
        assertThat(addressesStr).contains("Areas of Law: Family");
        assertThat(addressesStr).contains("Courts: High Court");
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
        assertThat(addressesStr).contains("Areas of Law: Crime");
        assertThat(addressesStr).contains("Courts: Crown Court");
    }

    @Test
    void shouldConvertJsonToCsv() {
        ArrayNode root = mapper.createArrayNode();
        ObjectNode court = root.addObject();
        court.put("name", "Test Court");
        court.put("slug", "test-court");

        String csv = csvUtil.convertJsonToCsv(root);

        assertThat(csv)
            .contains("name,lat,lon,number,cci_code,magistrate_code,slug,types,open,dx_number,areas_of_law,addresses");
        assertThat(csv).contains("Test Court");
        assertThat(csv).contains("test-court");
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
        assertThat(result.get("dx_number")).isEqualTo("DX 123");

        ArrayNode dxCodes = root.putArray("courtDxCodes");
        ObjectNode dx1 = dxCodes.addObject();
        dx1.put("dxCode", "DX 456");

        result = csvUtil.flattenCourtNode(root);
        assertThat(result.get("dx_number")).isEqualTo("DX 456");
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
        assertThat(result.get("open")).isEqualTo(true);
        assertThat(result.get("number")).isEqualTo(123);
        assertThat(result.get("cci_code")).isEqualTo(456);
        assertThat(result.get("magistrate_code")).isEqualTo(789);
        assertThat(result.get("lat")).isEqualTo(51.5);
        assertThat(result.get("lon")).isEqualTo(-0.1);
    }

    @Test
    void shouldHandleNullsInReadMethods() {
        ObjectNode root = mapper.createObjectNode();
        Map<String, Object> result = csvUtil.flattenCourtNode(root);

        assertThat(result.get("lat")).isNull();
        assertThat(result.get("number")).isNull();
        assertThat(result.get("open")).isEqualTo(false);
    }

    @Test
    void shouldThrowJsonConvertExceptionWhenCsvWritingFails() throws Exception {
        CsvMapper mockCsvMapper = mock(CsvMapper.class);
        CsvUtil utilWithMock = new CsvUtil(mockCsvMapper);

        when(mockCsvMapper.writer(org.mockito.ArgumentMatchers.any(
            com.fasterxml.jackson.dataformat.csv.CsvSchema.class)))
            .thenThrow(new RuntimeException("Mock failure"));

        ArrayNode root = mapper.createArrayNode();
        root.addObject();

        assertThatThrownBy(() -> utilWithMock.convertJsonToCsv(root))
            .isInstanceOf(JsonConvertException.class)
            .hasMessageContaining("Failed to convert JSON to CSV: Mock failure");
    }
}
