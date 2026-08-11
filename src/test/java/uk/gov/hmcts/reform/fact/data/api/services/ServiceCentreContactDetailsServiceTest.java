package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreContactDetailsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreContactDetailsServiceTest {

    @Mock
    private ServiceCentreContactDetailsRepository serviceCentreContactDetailsRepository;

    @Mock
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;

    @Mock
    private ServiceCentreService serviceCentreService;

    @InjectMocks
    private ServiceCentreContactDetailsService serviceCentreContactDetailsService;

    @Test
    void getContactDetailsReturnsParentScopedRecords() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .serviceCentreId(serviceCentreId)
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreContactDetailsRepository.findByServiceCentreId(serviceCentreId))
            .thenReturn(List.of(contactDetails));

        assertThat(serviceCentreContactDetailsService.getContactDetails(serviceCentreId))
            .containsExactly(contactDetails);
    }

    @Test
    void createContactDetailAllowsMissingDescriptionWhenNull() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        ServiceCentreContactDetails request = ServiceCentreContactDetails.builder()
            .email("test@example.com")
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreContactDetailsRepository.save(any(ServiceCentreContactDetails.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceCentreContactDetails result =
            serviceCentreContactDetailsService.createContactDetail(serviceCentreId, request);

        assertThat(result.getServiceCentreId()).isEqualTo(serviceCentreId);
        assertThat(result.getServiceCentre()).isEqualTo(serviceCentre);
        assertThat(result.getServiceCentreContactDescription()).isNull();
    }

    @Test
    void createContactDetailThrowsNotFoundWhenDescriptionMissing() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID contactDescriptionId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        ServiceCentreContactDetails request = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(contactDescriptionId)
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(contactDescriptionTypeRepository.findById(contactDescriptionId)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> serviceCentreContactDetailsService.createContactDetail(serviceCentreId, request)
        );
    }

    @Test
    void updateContactDetailAppliesNewValuesAndDescription() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID contactDescriptionId = UUID.randomUUID();
        ContactDescriptionType contactDescriptionType = new ContactDescriptionType();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        ServiceCentreContactDetails existing = ServiceCentreContactDetails.builder()
            .id(contactId)
            .serviceCentreId(serviceCentreId)
            .build();
        ServiceCentreContactDetails request = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(contactDescriptionId)
            .explanation("General")
            .email("updated@example.com")
            .phoneNumber("020 123 456")
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreContactDetailsRepository.findByIdAndServiceCentreId(contactId, serviceCentreId))
            .thenReturn(Optional.of(existing));
        when(contactDescriptionTypeRepository.findById(contactDescriptionId))
            .thenReturn(Optional.of(contactDescriptionType));
        when(serviceCentreContactDetailsRepository.save(existing)).thenReturn(existing);

        ServiceCentreContactDetails result =
            serviceCentreContactDetailsService.updateContactDetail(serviceCentreId, contactId, request);

        assertThat(result.getServiceCentreContactDescriptionId()).isEqualTo(contactDescriptionId);
        assertThat(result.getServiceCentreContactDescription()).isEqualTo(contactDescriptionType);
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void deleteContactDetailThrowsNotFoundWhenChildDoesNotBelongToParent() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreContactDetailsRepository.existsByIdAndServiceCentreId(contactId, serviceCentreId))
            .thenReturn(false);

        assertThrows(
            NotFoundException.class,
            () -> serviceCentreContactDetailsService.deleteContactDetail(serviceCentreId, contactId)
        );
    }

    @Test
    void deleteContactDetailDeletesParentScopedRecord() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreContactDetailsRepository.existsByIdAndServiceCentreId(contactId, serviceCentreId))
            .thenReturn(true);

        serviceCentreContactDetailsService.deleteContactDetail(serviceCentreId, contactId);

        verify(serviceCentreContactDetailsRepository).deleteByIdAndServiceCentreId(contactId, serviceCentreId);
    }
}
