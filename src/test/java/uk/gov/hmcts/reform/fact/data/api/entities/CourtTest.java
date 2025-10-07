package uk.gov.hmcts.reform.fact.data.api.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CourtTest {

    @Test
    void ensureRegionIdMatchesRegionAfterPostLoad() {
        var region = new Region();
        region.setId(UUID.randomUUID());
        region.setName("Eastern");
        region.setCountry("England");

        var court = new Court();
        court.setRegion(region);
        assertNull(court.getRegionId());

        court.postLoad();
        assertEquals(court.getRegionId(), region.getId());
    }
}
