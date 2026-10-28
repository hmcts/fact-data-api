package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.postgresql.PGConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportMetadata;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class PostcodeReferenceImportRepository {

    private static final long IMPORT_LOCK_ID = 0x4641_4354_4F4E_5350L;
    private static final String COPY_SQL = """
        COPY postcode_reference_stage (
            postcode,
            status,
            is_large_user,
            introduced_yyyymm,
            terminated_yyyymm,
            coordinate_quality,
            has_coordinates,
            latitude,
            longitude
        ) FROM STDIN WITH (FORMAT csv, HEADER true, NULL '')
        """;
    private static final String FIND_IMPORT_SQL = """
        SELECT source_version, onspd_release, record_count, imported_at
        FROM postcode_reference_import
        WHERE source_version = ?
        """;
    private static final String INSERT_IMPORT_SQL = """
        INSERT INTO postcode_reference_import (
            source_version, onspd_release, blob_etag, record_count, imported_at
        ) VALUES (?, ?, ?, ?, ?)
        """;
    private static final String REPLACE_LIVE_TABLE_SQL = """
        INSERT INTO postcode_reference (
            postcode,
            status,
            is_large_user,
            introduced_yyyymm,
            terminated_yyyymm,
            coordinate_quality,
            has_coordinates,
            latitude,
            longitude
        )
        SELECT
            postcode,
            status,
            is_large_user,
            introduced_yyyymm,
            terminated_yyyymm,
            coordinate_quality,
            has_coordinates,
            latitude,
            longitude
        FROM postcode_reference_stage
        """;

    private final JdbcTemplate jdbcTemplate;

    public PostcodeReferenceImportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Imports a previously unseen ONSPD source version through a staging table so invalid or partial data cannot
     * replace the live postcode reference table. The transaction-level advisory lock serialises requests across API
     * replicas.
     *
     * @param csvInputStream stream containing the reduced ONSPD CSV
     * @param metadata trusted blob metadata used to validate and identify the import
     * @return the completed import, or the existing import when the source version was already processed
     */
    @Transactional
    public PostcodeReferenceImportResult replaceIfRequired(
        InputStream csvInputStream,
        PostcodeReferenceImportMetadata metadata
    ) {
        try {
            return executeImport(csvInputStream, metadata);
        } catch (DataAccessException ex) {
            throw new PostcodeReferenceImportException("Failed to replace postcode reference data", ex);
        }
    }

    private PostcodeReferenceImportResult executeImport(
        InputStream csvInputStream,
        PostcodeReferenceImportMetadata metadata
    ) {
        acquireImportLock();
        Optional<PostcodeReferenceImportResult> previousImport = findImport(metadata.sourceVersion());
        if (previousImport.isPresent()) {
            return previousImport.get();
        }

        createStagingTable();
        long importedRecordCount = copyIntoStagingTable(csvInputStream);
        validateRecordCount(importedRecordCount, metadata.recordCount());
        replaceLiveTable();
        return recordImport(metadata);
    }

    private void acquireImportLock() {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", resultSet -> null, IMPORT_LOCK_ID);
    }

    private Optional<PostcodeReferenceImportResult> findImport(String sourceVersion) {
        return jdbcTemplate.query(FIND_IMPORT_SQL, resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(new PostcodeReferenceImportResult(
                false,
                resultSet.getString("source_version"),
                resultSet.getString("onspd_release"),
                resultSet.getLong("record_count"),
                resultSet.getTimestamp("imported_at").toInstant()
            ));
        }, sourceVersion);
    }

    private void createStagingTable() {
        jdbcTemplate.execute(
            "CREATE TEMP TABLE postcode_reference_stage "
                + "(LIKE postcode_reference INCLUDING ALL) ON COMMIT DROP"
        );
    }

    private long copyIntoStagingTable(InputStream csvInputStream) {
        Long importedRecordCount = jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try {
                return connection.unwrap(PGConnection.class).getCopyAPI().copyIn(COPY_SQL, csvInputStream);
            } catch (IOException ex) {
                throw new PostcodeReferenceImportException("Unable to read ONSPD CSV during import", ex);
            }
        });
        if (importedRecordCount == null) {
            throw new PostcodeReferenceImportException("ONSPD CSV import did not return a record count");
        }
        return importedRecordCount;
    }

    private void validateRecordCount(long actualRecordCount, long expectedRecordCount) {
        if (actualRecordCount < 1) {
            throw new PostcodeReferenceImportException("ONSPD CSV contained no postcode reference records");
        }
        if (actualRecordCount != expectedRecordCount) {
            throw new PostcodeReferenceImportException(
                "ONSPD record count mismatch: blob metadata expected " + expectedRecordCount
                    + " but CSV contained " + actualRecordCount
            );
        }
    }

    private void replaceLiveTable() {
        jdbcTemplate.update("DELETE FROM postcode_reference");
        jdbcTemplate.update(REPLACE_LIVE_TABLE_SQL);
    }

    private PostcodeReferenceImportResult recordImport(PostcodeReferenceImportMetadata metadata) {
        Instant importedAt = Instant.now();
        jdbcTemplate.update(
            INSERT_IMPORT_SQL,
            metadata.sourceVersion(),
            metadata.release(),
            metadata.blobEtag(),
            metadata.recordCount(),
            Timestamp.from(importedAt)
        );
        return new PostcodeReferenceImportResult(
            true,
            metadata.sourceVersion(),
            metadata.release(),
            metadata.recordCount(),
            importedAt
        );
    }

}
