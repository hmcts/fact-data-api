package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.List;
import java.util.Map;
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
    void getAreasOfLawForViewReturnsAreaDetailsWhenPresent() {
        UUID areaOfLawId = UUID.randomUUID();
        AreaOfLawType areaOfLawType = AreaOfLawType.builder()
            .id(areaOfLawId)
            .name("Family")
            .nameCy("Teulu")
            .build();
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();
        areasOfLaw.setAreasOfLawDetails(List.of(areaOfLawType));

        assertThat(areasOfLaw.getAreasOfLawForView()).containsExactly(areaOfLawType);
    }

    @Test
    void getAreasOfLawForViewReturnsAreaIdsWhenDetailsMissing() {
        UUID areaOfLawId = UUID.randomUUID();
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();
        areasOfLaw.setAreasOfLaw(List.of(areaOfLawId));

        assertThat(areasOfLaw.getAreasOfLawForView()).containsExactly(areaOfLawId);
    }

    @Test
    void getAreasOfLawForViewReturnsNullWhenNoAreasSet() {
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();

        assertThat(areasOfLaw.getAreasOfLawForView()).isNull();
    }

    @Test
    void setAreasOfLawAcceptsNull() {
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();

        areasOfLaw.setAreasOfLaw(null);

        assertThat(areasOfLaw.getAreasOfLaw()).isNull();
    }

    @Test
    void setAreasOfLawExtractsIdsFromSupportedRepresentations() {
        UUID fromString = UUID.randomUUID();
        UUID fromEntity = UUID.randomUUID();
        UUID fromUuidMap = UUID.randomUUID();
        UUID fromStringMap = UUID.randomUUID();
        UUID fromFallback = UUID.randomUUID();

        AreaOfLawType areaOfLawType = AreaOfLawType.builder().id(fromEntity).build();
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();

        areasOfLaw.setAreasOfLaw(List.of(
            fromString.toString(),
            areaOfLawType,
            Map.of("id", fromUuidMap),
            Map.of("id", fromStringMap.toString()),
            new StringBuilder(fromFallback.toString())
        ));

        assertThat(areasOfLaw.getAreasOfLaw()).containsExactly(
            fromString,
            fromEntity,
            fromUuidMap,
            fromStringMap,
            fromFallback
        );
    }

    @Test
    void returnsAuditSubjectMetadata() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentreAreasOfLaw areasOfLaw = new ServiceCentreAreasOfLaw();
        areasOfLaw.setServiceCentreId(serviceCentreId);

        assertThat(areasOfLaw.getAuditSubjectId()).isEqualTo(serviceCentreId);
        assertThat(areasOfLaw.getAuditSubjectType()).isEqualTo(SubjectType.SERVICE_CENTRE);
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
