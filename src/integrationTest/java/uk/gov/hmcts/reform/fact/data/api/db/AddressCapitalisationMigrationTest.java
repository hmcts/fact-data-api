package uk.gov.hmcts.reform.fact.data.api.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AddressCapitalisationMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldTitleCaseAllAddressFieldsInBothAddressTables() {
        String schema = "address_migration_" + UUID.randomUUID().toString().replace("-", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> tables = List.of("court_address", "service_centre_address");

        try {
            jdbcTemplate.execute("CREATE SCHEMA " + schema);
            for (String table : tables) {
                jdbcTemplate.execute("CREATE TABLE " + schema + "." + table + """
                    (id INTEGER PRIMARY KEY, address_line_1 TEXT, address_line_2 TEXT,
                     town_city TEXT, county TEXT, postcode TEXT, reference TEXT)
                    """);
                jdbcTemplate.update("INSERT INTO " + schema + "." + table + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    1, "10A JANE DOE STREET", "HMCTS, KING'S ROAD", "STOKE-ON-TRENT", "WEST MIDLANDS",
                    "ST1 3BP", "EPIM-123");
                jdbcTemplate.update("INSERT INTO " + schema + "." + table + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    2, "HMCTS McDonald House", "Admin's chosen CASE", "London", "Greater London",
                    "SW1A 1AA", "EPIM-456");
                jdbcTemplate.update("INSERT INTO " + schema + "." + table + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    3, "123", null, "", "  ", "SW1A 1AA", null);
                jdbcTemplate.update("INSERT INTO " + schema + "." + table + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    4, "ST. JOHN’S ROAD / NORTH & SOUTH", "  COURT  HOUSE  ",
                    "LLANFAIR-PWLLGWYNGYLL", "ÉLY", "LL61 5YZ", "EPIM-789");
                jdbcTemplate.update("INSERT INTO " + schema + "." + table + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    5, "lowercase house", "lowercase second line", "london", null,
                    "SW1A 1AA", "EPIM-000");
            }

            final Map<String, Object> emptyCourt = getAddress(jdbcTemplate, schema, "court_address", 3);
            final Map<String, Object> emptyServiceCentre =
                getAddress(jdbcTemplate, schema, "service_centre_address", 3);

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.59")
                .target("1.60")
                .load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

            for (String table : tables) {
                assertThat(getAddress(jdbcTemplate, schema, table, 1)).containsAllEntriesOf(Map.of(
                    "address_line_1", "10A Jane Doe Street",
                    "address_line_2", "Hmcts, King's Road",
                    "town_city", "Stoke-On-Trent",
                    "county", "West Midlands",
                    "postcode", "ST1 3BP",
                    "reference", "EPIM-123"
                ));
                assertThat(getAddress(jdbcTemplate, schema, table, 2)).containsAllEntriesOf(Map.of(
                    "address_line_1", "Hmcts Mcdonald House",
                    "address_line_2", "Admin's Chosen Case",
                    "town_city", "London",
                    "county", "Greater London",
                    "postcode", "SW1A 1AA",
                    "reference", "EPIM-456"
                ));
                assertThat(getAddress(jdbcTemplate, schema, table, 4)).containsAllEntriesOf(Map.of(
                    "address_line_1", "St. John’s Road / North & South",
                    "address_line_2", "  Court  House  ",
                    "town_city", "Llanfair-Pwllgwyngyll",
                    "county", "Ély",
                    "postcode", "LL61 5YZ"
                ));
                assertThat(getAddress(jdbcTemplate, schema, table, 5)).containsAllEntriesOf(Map.of(
                    "address_line_1", "Lowercase House",
                    "address_line_2", "Lowercase Second Line",
                    "town_city", "London"
                ));
            }
            assertThat(getAddress(jdbcTemplate, schema, "court_address", 3)).isEqualTo(emptyCourt);
            assertThat(getAddress(jdbcTemplate, schema, "service_centre_address", 3)).isEqualTo(emptyServiceCentre);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private Map<String, Object> getAddress(JdbcTemplate jdbcTemplate, String schema, String table, int id) {
        return jdbcTemplate.queryForMap("SELECT * FROM " + schema + "." + table + " WHERE id = ?", id);
    }
}
