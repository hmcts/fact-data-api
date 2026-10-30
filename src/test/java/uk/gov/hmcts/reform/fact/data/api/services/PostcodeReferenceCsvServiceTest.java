package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import uk.gov.hmcts.reform.fact.data.api.clients.OnspdClient;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.OnspdRelease;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeReferenceCsvServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Mock
    private OnspdClient onspdClient;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void writesLargeUserAndTerminatedPostcodesWithCoordinates() throws Exception {
        OnspdRelease release = release();
        when(onspdClient.getFilteredPostcodeCount(release)).thenReturn(2L);
        when(onspdClient.getPostcodeRecordsAfter(release, 0)).thenReturn(List.of(
            attributes(10, "G2  8GT", null, 1, "55.858872", "-4.260375"),
            attributes(20, "CW1 2DT", 202601, 0, null, null)
        ));
        Path output = temporaryDirectory.resolve("postcode-reference.csv");

        long count = new PostcodeReferenceCsvService(onspdClient).createReducedCsv(release, output);

        assertThat(count).isEqualTo(2);
        List<Map<String, String>> rows = readCsv(output);
        assertThat(rows.get(0))
            .containsEntry("postcode", "G2 8GT")
            .containsEntry("status", "current")
            .containsEntry("is_large_user", "true")
            .containsEntry("has_coordinates", "true");
        assertThat(rows.get(1))
            .containsEntry("status", "terminated")
            .containsEntry("terminated_yyyymm", "202601")
            .containsEntry("has_coordinates", "false");
    }

    @Test
    void rejectsPaginationThatStopsBeforeExpectedCount() {
        OnspdRelease release = release();
        PostcodeReferenceCsvService csvService = new PostcodeReferenceCsvService(onspdClient);
        Path output = temporaryDirectory.resolve("postcode-reference.csv");
        when(onspdClient.getFilteredPostcodeCount(release)).thenReturn(2L);
        when(onspdClient.getPostcodeRecordsAfter(release, 0)).thenReturn(List.of(
            attributes(10, "G2 8GT", null, 1, "55.858872", "-4.260375")));
        when(onspdClient.getPostcodeRecordsAfter(release, 10)).thenReturn(List.of());

        assertThatThrownBy(() -> csvService.createReducedCsv(release, output))
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("pagination ended after 1 of 2");
    }

    @Test
    void rejectsRowsOutsideRequestedFilter() {
        OnspdRelease release = release();
        PostcodeReferenceCsvService csvService = new PostcodeReferenceCsvService(onspdClient);
        Path output = temporaryDirectory.resolve("postcode-reference.csv");
        when(onspdClient.getFilteredPostcodeCount(release)).thenReturn(1L);
        when(onspdClient.getPostcodeRecordsAfter(release, 0)).thenReturn(List.of(
            attributes(10, "AB1 0AA", null, 0, "57.1", "-2.2")));

        assertThatThrownBy(() -> csvService.createReducedCsv(release, output))
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("outside the requested filter");
    }

    private List<Map<String, String>> readCsv(Path path) throws Exception {
        try (MappingIterator<Map<String, String>> rows = CsvMapper.builder().build()
            .readerFor(new TypeReference<Map<String, String>>() { })
            .with(CsvSchema.emptySchema().withHeader())
            .readValues(path.toFile())) {
            return rows.readAll();
        }
    }

    private JsonNode attributes(long objectId,
                                String postcode,
                                Integer terminated,
                                int userType,
                                String latitude,
                                String longitude) {
        return OBJECT_MAPPER.valueToTree(Map.of(
            "ObjectId", objectId,
            "pcds", postcode,
            "dointr", 201802,
            "doterm", terminated == null ? OBJECT_MAPPER.nullNode() : terminated,
            "usrtypind", userType,
            "gridind", 5,
            "lat", latitude == null ? OBJECT_MAPPER.nullNode() : latitude,
            "long", longitude == null ? OBJECT_MAPPER.nullNode() : longitude
        ));
    }

    private OnspdRelease release() {
        return new OnspdRelease("aug", "August 2026", YearMonth.of(2026, 8), "https://services.test", 3L);
    }
}
