package uk.gov.hmcts.reform.fact.data.api.repositories;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationSearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("All Location Search Repository")
@DisplayName("All Location Search Repository")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AllLocationSearchRepositoryTest {

    @Autowired
    private AllLocationSearchRepository allLocationSearchRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private ServiceCentreRepository serviceCentreRepository;

    @Autowired
    private CourtAddressRepository courtAddressRepository;

    @Autowired
    private ServiceCentreAddressRepository serviceCentreAddressRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditUserContext auditUserContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID regionId;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        auditUserContext.suppressAudit();
        regionId = regionRepository.save(Region.builder()
            .name("All Location Search Region")
            .country("England")
            .build()).getId();
    }

    @AfterEach
    void tearDown() {
        auditUserContext.clear();
    }

    @Test
    void searchOpenByNameOrAddressRanksBothLocationTypesAndExcludesClosedAndWriteOnlyAddresses() {
        final String query = "Ranktoken";

        final ServiceCentre postcodeMatch = saveServiceCentre("Zulu Postcode Service Centre", true);
        serviceCentreRepository.flush();
        jdbcTemplate.update(
            """
                INSERT INTO service_centre_address
                    (id, service_centre_id, address_line_1, town_city, county, postcode, address_type)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            postcodeMatch.getId(),
            "Other address",
            "Other town",
            "Other county",
            query.substring(0, 4) + " " + query.substring(4),
            AddressType.VISIT_US.name()
        );

        final Court nameMatch = saveCourt(query + " Court", true);

        final ServiceCentre townMatch = saveServiceCentre("Beta Town Service Centre", true);
        saveServiceCentreAddress(
            townMatch,
            AddressType.VISIT_OR_CONTACT_US,
            "Other address",
            null,
            query,
            "Other county",
            "SW1A 1AA"
        );

        final Court addressLine1Match = saveCourt("Gamma Address One Court", true);
        saveCourtAddress(addressLine1Match, AddressType.VISIT_US, query, null, "Other town", "Other county");

        final ServiceCentre addressLine2Match = saveServiceCentre("Delta Address Two Service Centre", true);
        saveServiceCentreAddress(
            addressLine2Match,
            AddressType.VISIT_US,
            "Other address",
            query,
            "Other town",
            "Other county",
            "SW1A 1AA"
        );

        final Court countyMatch = saveCourt("Alpha County Court", true);
        saveCourtAddress(countyMatch, AddressType.VISIT_US, "Other address", null, "Other town", query);

        final ServiceCentre secondCountyMatch = saveServiceCentre("Omega County Service Centre", true);
        saveServiceCentreAddress(
            secondCountyMatch,
            AddressType.VISIT_US,
            "Other address",
            null,
            "Other town",
            query,
            "SW1A 1AA"
        );

        saveCourt(query + " Closed Court", false);
        saveServiceCentre(query + " Closed Service Centre", false);

        final ServiceCentre writeOnly = saveServiceCentre("Write Only Service Centre", true);
        saveServiceCentreAddress(
            writeOnly,
            AddressType.WRITE_TO_US,
            query,
            null,
            "Other town",
            "Other county",
            "SW1A 1AA"
        );

        final List<AllLocationSearchResult> results = allLocationSearchRepository.searchOpenByNameOrAddress(query);

        assertThat(results)
            .extracting(AllLocationSearchResult::getId)
            .containsExactly(
                postcodeMatch.getId(),
                nameMatch.getId(),
                townMatch.getId(),
                addressLine1Match.getId(),
                addressLine2Match.getId(),
                countyMatch.getId(),
                secondCountyMatch.getId()
            );
        assertThat(results)
            .extracting(AllLocationSearchResult::getLocationType)
            .containsExactly(
                "SERVICE_CENTRE",
                "COURT",
                "SERVICE_CENTRE",
                "COURT",
                "SERVICE_CENTRE",
                "COURT",
                "SERVICE_CENTRE"
            );
    }

    private Court saveCourt(String name, boolean open) {
        return courtRepository.save(Court.builder()
            .name(name)
            .slug(UUID.randomUUID().toString())
            .open(open)
            .regionId(regionId)
            .build());
    }

    private ServiceCentre saveServiceCentre(String name, boolean open) {
        return serviceCentreRepository.save(ServiceCentre.builder()
            .name(name)
            .slug(UUID.randomUUID().toString())
            .open(open)
            .regionId(regionId)
            .build());
    }

    private void saveCourtAddress(Court court,
                                  AddressType addressType,
                                  String addressLine1,
                                  String addressLine2,
                                  String townCity,
                                  String county) {
        courtAddressRepository.save(CourtAddress.builder()
            .courtId(court.getId())
            .addressType(addressType)
            .addressLine1(addressLine1)
            .addressLine2(addressLine2)
            .townCity(townCity)
            .county(county)
            .postcode("SW1A 1AA")
            .build());
    }

    private void saveServiceCentreAddress(ServiceCentre serviceCentre,
                                          AddressType addressType,
                                          String addressLine1,
                                          String addressLine2,
                                          String townCity,
                                          String county,
                                          String postcode) {
        serviceCentreAddressRepository.save(ServiceCentreAddress.builder()
            .serviceCentreId(serviceCentre.getId())
            .addressType(addressType)
            .addressLine1(addressLine1)
            .addressLine2(addressLine2)
            .townCity(townCity)
            .county(county)
            .postcode(postcode)
            .build());
    }
}
