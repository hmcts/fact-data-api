package uk.gov.hmcts.reform.fact.data.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
public class RegionRepositoryTest {

    @Autowired
    private RegionRepository regionRepository;

    @Test
    public void shouldSaveAndLoadRegionEntity() {
        Region region = new Region();
        region.setName("Test Region");
        region.setCountry("Test Country");

        var savedRegion = regionRepository.save(region);
        var foundRegion = regionRepository.findById(savedRegion.getId()).orElse(null);
        assertNotNull(foundRegion);
        assertNotNull(foundRegion.getId());
        assertEquals("Test Region", foundRegion.getName());
        assertEquals("Test Country", foundRegion.getCountry());
    }
}
