package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCentreAddressTest {

    @Test
    void returnsAuditMetadata() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentreAddress address = new ServiceCentreAddress();
        address.setServiceCentreId(serviceCentreId);

        assertThat(address.getAuditSubjectId()).isEqualTo(serviceCentreId);
        assertThat(address.getAuditSubjectType()).isEqualTo(SubjectType.SERVICE_CENTRE);
    }
}

