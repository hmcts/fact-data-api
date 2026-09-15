package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCentreTest {

    @Test
    void returnsAuditMetadata() {
        UUID id = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(id).build();

        assertThat(serviceCentre.getAuditSubjectId()).isEqualTo(id);
        assertThat(serviceCentre.getAuditSubjectType()).isEqualTo(SubjectType.SERVICE_CENTRE);
    }
}

