package uk.gov.hmcts.reform.fact.data.api.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Built-in feature which saves service's swagger specs in temporary directory.
 * Each CI run on master should automatically save and upload (if updated) documentation.
 * This test is specifically for the APIM version of the OpenAPI spec, which differs from the standard spec.
 */
@Feature("OpenAPI Publisher")
@DisplayName("OpenAPI Publisher")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OpenAPIPublisherApimTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Generate swagger documentation for APIM")
    @Test
    void generateDocs() throws Exception {
        byte[] specs = mvc.perform(get("/v3/api-docs/apim"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertTrue(specs.length > 0, "Generated OpenAPI spec should not be empty");

        JsonNode paths = objectMapper.readTree(specs).path("paths");
        assertFalse(paths.isEmpty(), "Generated OpenAPI spec should contain APIM paths");
        assertTrue(
            StreamSupport.stream(paths.spliterator(), false).noneMatch(JsonNode::isEmpty),
            "Generated OpenAPI spec should not contain paths without operations"
        );
        assertTrue(paths.has("/courts/name/v1"), "APIM spec should include court lookup by name");
        assertTrue(paths.has("/search/courts/v1/postcode"), "APIM spec should include court postcode search");

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/openapi-specs.json"))) {
            outputStream.write(specs);
        }

        assertTrue(Files.exists(Paths.get("/tmp/openapi-specs.json")), "OpenAPI spec file should be created");
    }
}
