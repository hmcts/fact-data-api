package uk.gov.hmcts.reform.fact.data.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// using @SpringBootTest as @DataJpaTest doesn't enable @PostLoad
// or jakarta.validation handling
@SpringBootTest
@ActiveProfiles("test")
public class CourtRepositoryTest {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    public void shouldSaveAndLoadCourtEntity() {

        final ZonedDateTime nowish = ZonedDateTime.now().minusSeconds(1);

        // Create and save a Region
        Region region = new Region();
        region.setName("Test Region");
        region = regionRepository.save(region);

        // Create and save a Court
        Court court = Court.builder()
            .name("Test Court")
            .slug("test-court")
            .open(true).temporaryUrgentNotice("Urgent notice")
            .regionId(region.getId())
            .isServiceCentre(true)
            .openOnCath(true)
            .mrdId(UUID.randomUUID().toString())
            .build();

        Court savedCourt = courtRepository.save(court);

        // Retrieve and inspect
        Court foundCourt = courtRepository.findById(savedCourt.getId()).orElse(null);
        assertNotNull(foundCourt);
        assertEquals("Test Court", foundCourt.getName());
        assertNotNull(foundCourt.getRegion());
        assertEquals(region.getId(), foundCourt.getRegionId());
        assertNotNull(foundCourt.getCreatedAt());
        assertTrue(nowish.isBefore(foundCourt.getCreatedAt()));
        assertNotNull(foundCourt.getLastUpdatedAt());
        assertTrue(nowish.isBefore(foundCourt.getLastUpdatedAt()));
    }
}
