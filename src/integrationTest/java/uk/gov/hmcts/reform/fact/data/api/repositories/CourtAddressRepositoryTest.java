package uk.gov.hmcts.reform.fact.data.api.repositories;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Court Address Repository")
@DisplayName("Court Address Repository")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourtAddressRepositoryTest {

    @Autowired
    private CourtAddressRepository courtAddressRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;

    @Autowired
    private AreaOfLawTypeRepository areaOfLawTypeRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditUserContext auditUserContext;

    private UUID regionId;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        auditUserContext.suppressAudit();
        regionId = regionRepository.save(Region.builder()
            .name("Court Address Search Region")
            .country("England")
            .build()).getId();
    }

    @AfterEach
    void tearDown() {
        auditUserContext.clear();
    }

    @Test
    void findFamilyNonRegionalByLocalAuthorityLimitsResultsAfterOrderingByDistance() {
        UUID areaOfLawId = areaOfLawTypeRepository.findAll().getFirst().getId();
        UUID localAuthorityId = UUID.randomUUID();

        Court firstCourt = saveCourt("First Family Court", "first-family-court");
        Court secondCourt = saveCourt("Second Family Court", "second-family-court");
        Court distantLowerUuidCourt = firstCourt.getId().toString().compareTo(secondCourt.getId().toString()) < 0
            ? firstCourt
            : secondCourt;
        Court nearbyHigherUuidCourt = distantLowerUuidCourt == firstCourt ? secondCourt : firstCourt;

        saveAddress(distantLowerUuidCourt, "50", "50");
        saveLocalAuthorityMapping(distantLowerUuidCourt, areaOfLawId, localAuthorityId);

        saveAddress(nearbyHigherUuidCourt, "0.1", "0.1");
        saveLocalAuthorityMapping(nearbyHigherUuidCourt, areaOfLawId, localAuthorityId);

        List<CourtWithDistance> results = courtAddressRepository.findFamilyNonRegionalByLocalAuthority(
            0,
            0,
            areaOfLawId,
            localAuthorityId,
            1
        );

        assertThat(results)
            .extracting(CourtWithDistance::getCourtId)
            .containsExactly(nearbyHigherUuidCourt.getId());
    }

    private Court saveCourt(String name, String slug) {
        return courtRepository.saveAndFlush(Court.builder()
            .name(name)
            .slug(slug)
            .open(true)
            .regionId(regionId)
            .build());
    }

    private void saveAddress(Court court, String lat, String lon) {
        courtAddressRepository.saveAndFlush(CourtAddress.builder()
            .courtId(court.getId())
            .addressLine1("Test address")
            .townCity("Test town")
            .postcode("SW1A 1AA")
            .lat(new BigDecimal(lat))
            .lon(new BigDecimal(lon))
            .addressType(AddressType.VISIT_US)
            .build());
    }

    private void saveLocalAuthorityMapping(Court court, UUID areaOfLawId, UUID localAuthorityId) {
        courtLocalAuthoritiesRepository.saveAndFlush(CourtLocalAuthorities.builder()
            .courtId(court.getId())
            .areaOfLawId(areaOfLawId)
            .localAuthorityIds(List.of(localAuthorityId))
            .build());
    }
}
