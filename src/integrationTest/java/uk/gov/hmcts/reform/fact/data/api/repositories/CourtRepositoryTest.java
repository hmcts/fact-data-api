package uk.gov.hmcts.reform.fact.data.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;

import java.time.ZonedDateTime;

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

        // Create and save a Region
        Region region = new Region();
        region.setName("Test Region");
        region = regionRepository.save(region);

        // Create and save a Court
        Court court = new Court();
        court.setName("Test Court");
        court.setSlug("test-court");
        court.setOpen(true);
        court.setTemporaryUrgentNotice("Urgent notice");
        court.setCreatedAt(ZonedDateTime.now());
        court.setLastUpdatedAt(ZonedDateTime.now());
        court.setRegion(region);
        court.setIsServiceCentre(false);
        court.setOpenOnCath(true);
        court.setMrdId("MRD123");

        var savedCourt = courtRepository.save(court);

        // Retrieve and inspect
        var foundCourt = courtRepository.findById(savedCourt.getId()).orElse(null);
        assertNotNull(foundCourt);
        assertEquals("Test Court", foundCourt.getName());
        assertNotNull(foundCourt.getRegion());
        assertEquals("Test Region", foundCourt.getRegion().getName());

        // tests that @PostLoad is working
        assertEquals(region.getId(), foundCourt.getRegionId());
    }
}
