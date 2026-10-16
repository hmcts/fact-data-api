package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.postgresql.PGConnection;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportMetadata;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

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

    private final DataSource dataSource;

    public PostcodeReferenceImportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
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
    public PostcodeReferenceImportResult replaceIfRequired(
        InputStream csvInputStream,
        PostcodeReferenceImportMetadata metadata
    ) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                acquireImportLock(connection);
                Optional<PostcodeReferenceImportResult> previousImport = findImport(
                    connection,
                    metadata.sourceVersion()
                );
                if (previousImport.isPresent()) {
                    connection.commit();
                    return previousImport.get();
                }

                createStagingTable(connection);
                long importedRecordCount = copyIntoStagingTable(connection, csvInputStream);
                validateRecordCount(importedRecordCount, metadata.recordCount());
                replaceLiveTable(connection);
                PostcodeReferenceImportResult result = recordImport(connection, metadata);
                connection.commit();
                return result;
            } catch (SQLException | IOException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        } catch (SQLException | IOException ex) {
            throw new PostcodeReferenceImportException("Failed to replace postcode reference data", ex);
        }
    }

    private void acquireImportLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, IMPORT_LOCK_ID);
            statement.execute();
        }
    }

    private Optional<PostcodeReferenceImportResult> findImport(Connection connection, String sourceVersion)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_IMPORT_SQL)) {
            statement.setString(1, sourceVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
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
            }
        }
    }

    private void createStagingTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "CREATE TEMP TABLE postcode_reference_stage "
                + "(LIKE postcode_reference INCLUDING ALL) ON COMMIT DROP")) {
            statement.execute();
        }
    }

    private long copyIntoStagingTable(Connection connection, InputStream csvInputStream)
        throws SQLException, IOException {
        return connection.unwrap(PGConnection.class).getCopyAPI().copyIn(COPY_SQL, csvInputStream);
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

    private void replaceLiveTable(Connection connection) throws SQLException {
        try (PreparedStatement truncate = connection.prepareStatement("TRUNCATE TABLE postcode_reference");
             PreparedStatement insert = connection.prepareStatement(
                 "INSERT INTO postcode_reference SELECT * FROM postcode_reference_stage")) {
            truncate.execute();
            insert.executeUpdate();
        }
    }

    private PostcodeReferenceImportResult recordImport(
        Connection connection,
        PostcodeReferenceImportMetadata metadata
    ) throws SQLException {
        Instant importedAt = Instant.now();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_IMPORT_SQL)) {
            statement.setString(1, metadata.sourceVersion());
            statement.setString(2, metadata.release());
            statement.setString(3, metadata.blobEtag());
            statement.setLong(4, metadata.recordCount());
            statement.setTimestamp(5, Timestamp.from(importedAt));
            statement.executeUpdate();
        }
        return new PostcodeReferenceImportResult(
            true,
            metadata.sourceVersion(),
            metadata.release(),
            metadata.recordCount(),
            importedAt
        );
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
