package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserFavouritesMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationInitialisesFavouriteArraysAndIndexesOnUsers() {
        final String schema = "favourite_migration_" + UUID.randomUUID().toString().replace("-", "");
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final UUID userId = UUID.randomUUID();

        try {
            jdbcTemplate.execute("CREATE SCHEMA " + schema);
            jdbcTemplate.execute("CREATE TABLE " + schema + ".users (id UUID PRIMARY KEY, favourite_courts UUID[])");
            jdbcTemplate.execute(
                "INSERT INTO " + schema + ".users (id, favourite_courts) VALUES ('" + userId + "', NULL)"
            );

            Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.45")
                .load()
                .migrate();

            assertThat(jdbcTemplate.queryForObject(
                "SELECT cardinality(favourite_courts) FROM " + schema + ".users WHERE id = ?",
                Integer.class,
                userId
            )).isZero();
            assertThat(jdbcTemplate.queryForObject(
                "SELECT cardinality(favourite_service_centres) FROM " + schema + ".users WHERE id = ?",
                Integer.class,
                userId
            )).isZero();
            assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? "
                    + "AND table_name = 'user_favourites'",
                Long.class,
                schema
            )).isZero();
            assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = ? "
                    + "AND indexname IN ('users_favourite_courts_idx', 'users_favourite_service_centres_idx')",
                Long.class,
                schema
            )).isEqualTo(2);
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
