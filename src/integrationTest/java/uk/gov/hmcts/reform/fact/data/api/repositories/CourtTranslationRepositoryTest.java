package uk.gov.hmcts.reform.fact.data.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// using @SpringBootTest as @DataJpaTest doesn't enable @PostLoad
// or jakarta.validation handling
@SpringBootTest(
        properties = "spring.config.name=application-test"
)
@ActiveProfiles("test")
public class CourtTranslationRepositoryTest {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private CourtTranslationRepository courtTranslationRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    public void shouldSaveAndLoadCourtTranslationEntity() {


        // Create and save a Region
        Region region = new Region();
        region.setName("Test Region");
        region = regionRepository.save(region);

        // Create and save a Court
        Court court = new Court();
        court.setName("Test Court");
        court.setSlug("test-court");
        court.setOpen(true);
        court.setWarningNotice("Urgent notice");
        court.setRegionId(region.getId());
        court.setIsServiceCentre(false);
        court.setOpenOnCath(true);
        court.setMrdId("MRD123");

        court = courtRepository.save(court);

        // create and save a translation
        var translation = new CourtTranslation();
        translation.setCourtId(court.getId());
        translation.setEmail("me@here.com");
        translation.setPhoneNumber("01234 433222");

        var savedTranslation = courtTranslationRepository.save(translation);

        // create a translation for comparison
        var translation1 = CourtTranslation.builder()
            .id(savedTranslation.getId())
            .courtId(court.getId())
            .court(null) // gets lazy loaded
            .email(translation.getEmail())
            .phoneNumber(translation.getPhoneNumber())
            .build();

        // should equal what got saved and the court shouldn't be loaded yet
        assertEquals(translation1, savedTranslation);

        // lazy load the court
        translation1.setCourt(savedTranslation.getCourt());

        // make sure it still marries up now that we've added pulled the court
        assertEquals(translation1, savedTranslation);

        var foundTranslation = courtTranslationRepository.findById(savedTranslation.getId()).orElse(null);

        assertNotNull(foundTranslation);
        assertEquals("me@here.com", foundTranslation.getEmail());
        assertEquals("01234 433222", foundTranslation.getPhoneNumber());
        assertEquals(court.getId(), foundTranslation.getCourtId());

    }
}

