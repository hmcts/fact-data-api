package uk.gov.hmcts.reform.fact.data.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;

import java.time.ZonedDateTime;

import jakarta.validation.ValidationException;
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

    @Test
    public void shouldSaveAndLoadTranslationEntity() {
        var court = new Court();
        court.setName("Court 1");
        court.setCreatedAt(ZonedDateTime.now());
        court.setLastUpdatedAt(ZonedDateTime.now());
        court = courtRepository.save(court);

        var translation = new Translation();
        translation.setEmail("me@here.com");
        translation.setPhoneNumber("+44 01234 433222");

        // test that our pre-persist validation is working
        assertThrows(ValidationException.class, () -> translationRepository.save(translation));

        // when the above fails, the id needs to be removed because the next attempt to save
        // will get upset that and ID has already been assigned.
        translation.setId(null);
        translation.setCourt(court);

        var savedTranslation = translationRepository.save(translation);

        var foundTranslation = translationRepository.findById(savedTranslation.getId()).orElse(null);

        assertNotNull(foundTranslation);
        assertNotNull(foundTranslation.getCourt());
        assertEquals("me@here.com",  foundTranslation.getEmail());
        assertEquals("+44 01234 433222",  foundTranslation.getPhoneNumber());
        // postLoad testing
        assertEquals(court.getId(), foundTranslation.getCourtId());

    }

}
