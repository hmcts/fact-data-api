package uk.gov.hmcts.reform.fact.functional.helpers;

import uk.gov.hmcts.reform.fact.functional.config.TestConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper for direct database queries.
 * Only used when no API endpoint exists (e.g., fetching region IDs).
 */
public final class DatabaseHelper {

    private DatabaseHelper() {
    }

    /**
     * Gets a connection to the test database.
     */
    private static Connection getConnection(final TestConfig config) throws SQLException {
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }

    /**
     * Fetches any region ID from the database.
     * Regions are pre-seeded by Flyway migrations.
     */
    public static String getAnyRegionId(final TestConfig config) {
        final String query = "SELECT id FROM region LIMIT 1";
        try (Connection conn = getConnection(config);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getString("id");
            }
            throw new RuntimeException("No regions found in database");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch region ID", e);
        }
    }
}
