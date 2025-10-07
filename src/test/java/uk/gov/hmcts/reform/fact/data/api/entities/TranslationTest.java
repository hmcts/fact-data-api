package uk.gov.hmcts.reform.fact.data.api.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TranslationTest {

    @Test
    void ensureCourtIdMatchesCourtAfterPostLoad() {
        var court = new Court();
        court.setId(UUID.randomUUID());

        var translation = new Translation();
        translation.setCourt(court);
        assertNull(translation.getCourtId());

        translation.postLoad();
        assertEquals(translation.getCourtId(), court.getId());
    }

}
