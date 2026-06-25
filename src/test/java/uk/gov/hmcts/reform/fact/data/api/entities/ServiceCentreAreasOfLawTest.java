package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCentreAreasOfLawTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void deserializesExpandedAreaOfLawDetailsToAreaIds() {
        UUID areaOfLawId = UUID.randomUUID();
        String json = """
            {
              "areasOfLaw": [
                {
                  "id": "%s",
                  "name": "Family",
                  "nameCy": "Teulu"
                }
              ]
            }
            """.formatted(areaOfLawId);

        ServiceCentreAreasOfLaw areasOfLaw = mapper.readValue(json, ServiceCentreAreasOfLaw.class);

        assertThat(areasOfLaw.getAreasOfLaw()).containsExactly(areaOfLawId);
    }

    @Test
    void deserializesAreaOfLawIds() {
        UUID areaOfLawId = UUID.randomUUID();
        String json = """
            {
              "areasOfLaw": ["%s"]
            }
            """.formatted(areaOfLawId);

        ServiceCentreAreasOfLaw areasOfLaw = mapper.readValue(json, ServiceCentreAreasOfLaw.class);

        assertThat(areasOfLaw.getAreasOfLaw()).containsExactly(areaOfLawId);
    }

    @Test
    void setsAreaOfLawIdsDirectly() {
        UUID areaOfLawId = UUID.randomUUID();
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();

        areasOfLaw.setAreasOfLaw(List.of(areaOfLawId));

        assertThat(areasOfLaw.getAreasOfLaw()).containsExactly(areaOfLawId);
    }

    @Test
    void serviceCentreDetailsDeserializesExpandedAreaOfLawDetailsToAreaIds() {
        UUID serviceCentreAreasOfLawId = UUID.randomUUID();
        UUID areaOfLawId = UUID.randomUUID();
        String json = """
            {
              "serviceCentreAreasOfLaw": [
                {
                  "id": "%s",
                  "areasOfLaw": [
                    {
                      "id": "%s",
                      "name": "Family",
                      "nameCy": "Teulu"
                    }
                  ]
                }
              ]
            }
            """.formatted(serviceCentreAreasOfLawId, areaOfLawId);

        ServiceCentreDetails serviceCentreDetails = mapper.readValue(json, ServiceCentreDetails.class);

        assertThat(serviceCentreDetails.getServiceCentreAreasOfLaw()).hasSize(1);
        assertThat(serviceCentreDetails.getServiceCentreAreasOfLaw().getFirst().getAreasOfLaw())
            .containsExactly(areaOfLawId);
    }
}
