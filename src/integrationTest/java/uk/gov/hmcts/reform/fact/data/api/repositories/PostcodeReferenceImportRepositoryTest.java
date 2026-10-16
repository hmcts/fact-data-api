package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportMetadata;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PostcodeReferenceImportRepositoryTest {

    private static final String HEADER = "postcode,status,is_large_user,introduced_yyyymm,terminated_yyyymm,"
        + "coordinate_quality,has_coordinates,latitude,longitude\n";

    @Autowired
    private PostcodeReferenceImportRepository importRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM postcode_reference_import");
        jdbcTemplate.update("DELETE FROM postcode_reference");
        jdbcTemplate.update("""
            INSERT INTO postcode_reference (
                postcode, status, is_large_user, introduced_yyyymm, terminated_yyyymm,
                coordinate_quality, has_coordinates, latitude, longitude
            ) VALUES ('OLD 1AA', 'current', true, '202001', NULL, 1, true, 51.0, -1.0)
            """);
    }

    @Test
    void replacesLiveDataAndRecordsImportHistory() {
        String csv = HEADER
            + "M60 9DJ,current,true,202001,,1,true,53.4808,-2.2426\n"
            + "CW1 2DT,terminated,false,198001,202401,1,true,53.0996,-2.4438\n";
        PostcodeReferenceImportMetadata metadata = metadata("source-version-1", 2);

        PostcodeReferenceImportResult result = importRepository.replaceIfRequired(stream(csv), metadata);

        assertThat(result.imported()).isTrue();
        assertThat(result.sourceVersion()).isEqualTo("source-version-1");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postcode_reference", Long.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM postcode_reference WHERE postcode = 'OLD 1AA'", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT record_count FROM postcode_reference_import WHERE source_version = 'source-version-1'",
            Long.class
        )).isEqualTo(2);
    }

    @Test
    void leavesDataUnchangedWhenSourceVersionWasAlreadyImported() {
        String firstCsv = HEADER + "M60 9DJ,current,true,202001,,1,true,53.4808,-2.2426\n";
        PostcodeReferenceImportMetadata metadata = metadata("source-version-1", 1);
        importRepository.replaceIfRequired(stream(firstCsv), metadata);

        String secondCsv = HEADER + "G2 8GT,current,true,202001,,1,true,55.8588,-4.2603\n";
        PostcodeReferenceImportResult result = importRepository.replaceIfRequired(stream(secondCsv), metadata);

        assertThat(result.imported()).isFalse();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM postcode_reference WHERE postcode = 'M60 9DJ'", Long.class)).isOne();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM postcode_reference WHERE postcode = 'G2 8GT'", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postcode_reference_import", Long.class)).isOne();
    }

    @Test
    void rollsBackWhenBlobRecordCountDoesNotMatchCsv() {
        String csv = HEADER + "M60 9DJ,current,true,202001,,1,true,53.4808,-2.2426\n";

        assertThatThrownBy(() -> importRepository.replaceIfRequired(
            stream(csv), metadata("source-version-1", 2)))
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("record count mismatch");

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postcode_reference", Long.class)).isOne();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM postcode_reference WHERE postcode = 'OLD 1AA'", Long.class)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postcode_reference_import", Long.class)).isZero();
    }

    private PostcodeReferenceImportMetadata metadata(String sourceVersion, long recordCount) {
        return new PostcodeReferenceImportMetadata(sourceVersion, "2026-08", "etag-value", recordCount);
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
