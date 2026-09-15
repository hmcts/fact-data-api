package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCentreContactDetailsTest {

    @Test
    void getServiceCentreContactDescriptionForViewPrefersTransientDetails() {
        ContactDescriptionType transientDetails = ContactDescriptionType.builder()
            .id(UUID.randomUUID())
            .name("Phone")
            .nameCy("Ffon")
            .build();
        ContactDescriptionType persistedDetails = ContactDescriptionType.builder()
            .id(UUID.randomUUID())
            .name("Email")
            .nameCy("E-bost")
            .build();

        ServiceCentreContactDetails contactDetails = new ServiceCentreContactDetails();
        contactDetails.setServiceCentreContactDescriptionDetails(transientDetails);
        contactDetails.setServiceCentreContactDescription(persistedDetails);

        assertThat(contactDetails.getServiceCentreContactDescriptionForView()).isEqualTo(transientDetails);
    }

    @Test
    void getServiceCentreContactDescriptionForViewFallsBackToPersistedDetails() {
        ContactDescriptionType persistedDetails = ContactDescriptionType.builder()
            .id(UUID.randomUUID())
            .name("Email")
            .nameCy("E-bost")
            .build();

        ServiceCentreContactDetails contactDetails = new ServiceCentreContactDetails();
        contactDetails.setServiceCentreContactDescription(persistedDetails);

        assertThat(contactDetails.getServiceCentreContactDescriptionForView()).isEqualTo(persistedDetails);
    }

    @Test
    void returnsAuditMetadata() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentreContactDetails contactDetails = new ServiceCentreContactDetails();
        contactDetails.setServiceCentreId(serviceCentreId);

        assertThat(contactDetails.getAuditSubjectId()).isEqualTo(serviceCentreId);
        assertThat(contactDetails.getAuditSubjectType()).isEqualTo(SubjectType.SERVICE_CENTRE);
    }
}

