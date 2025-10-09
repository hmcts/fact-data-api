package uk.gov.hmcts.reform.fact.data.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// using @SpringBootTest as @DataJpaTest doesn't enable @PostLoad
// or jakarta.validation handling
@SpringBootTest
@ActiveProfiles("test")
public class TranslationRepositoryTest {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    public void shouldSaveAndLoadTranslationEntity() {

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
        court.setRegionId(region.getId());
        court.setIsServiceCentre(false);
        court.setOpenOnCath(true);
        court.setMrdId("MRD123");

        court = courtRepository.save(court);

        var translation = new Translation();
        translation.setCourtId(court.getId());
        translation.setEmail("me@here.com");
        translation.setPhoneNumber("01234567890");

        var savedTranslation = translationRepository.save(translation);

        var foundTranslation = translationRepository.findById(savedTranslation.getId()).orElse(null);

        assertNotNull(foundTranslation);
        assertEquals("me@here.com", foundTranslation.getEmail());
        assertEquals("01234567890", foundTranslation.getPhoneNumber());
        assertEquals(court.getId(), foundTranslation.getCourtId());

    }

}
