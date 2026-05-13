package uk.gov.hmcts.reform.fact.data.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
