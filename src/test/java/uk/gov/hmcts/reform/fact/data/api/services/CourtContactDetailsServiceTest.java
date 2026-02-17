package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtContactDetailsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtContactDetailsServiceTest {

    @Mock
    private CourtContactDetailsRepository courtContactDetailsRepository;

    @Mock
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtContactDetailsService courtContactDetailsService;

    private UUID courtId;
    private UUID contactId;
    private Court court;
    private CourtContactDetails contactDetail;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        contactId = UUID.randomUUID();

        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        contactDetail = CourtContactDetails.builder()
            .id(contactId)
            .courtId(courtId)
            .explanation("General enquiries")
            .email("enquiries@test.com")
            .phoneNumber("01234567890")
            .build();
    }

    @Test
    void getContactDetailsReturnsRecords() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.findByCourtId(courtId)).thenReturn(List.of(contactDetail));

        List<CourtContactDetails> result = courtContactDetailsService.getContactDetails(courtId);

        assertThat(result).containsExactly(contactDetail);
        verify(courtContactDetailsRepository).findByCourtId(courtId);
    }

    @Test
    void getContactDetailReturnsRecord() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.findByIdAndCourtId(contactId, courtId))
            .thenReturn(Optional.of(contactDetail));

        CourtContactDetails result = courtContactDetailsService.getContactDetail(courtId, contactId);

        assertThat(result).isEqualTo(contactDetail);
    }

    @Test
    void getContactDetailThrowsNotFoundWhenMissing() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.findByIdAndCourtId(contactId, courtId))
            .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtContactDetailsService.getContactDetail(courtId, contactId)
        );

        assertThat(exception.getMessage()).contains(contactId.toString());
    }

    @Test
    void createContactDetailSetsCourtAndDescription() {
        UUID descriptionId = UUID.randomUUID();
        ContactDescriptionType descriptionType = new ContactDescriptionType();
        descriptionType.setId(descriptionId);

        CourtContactDetails newDetail = CourtContactDetails.builder()
            .courtContactDescriptionId(descriptionId)
            .explanation("Phone enquiries")
            .email("info@test.com")
            .phoneNumber("09876543210")
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(contactDescriptionTypeRepository.findById(descriptionId)).thenReturn(Optional.of(descriptionType));
        when(courtContactDetailsRepository.save(any(CourtContactDetails.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtContactDetails result = courtContactDetailsService.createContactDetail(courtId, newDetail);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getCourt()).isEqualTo(court);
        assertThat(result.getCourtContactDescription()).isEqualTo(descriptionType);
        verify(courtContactDetailsRepository).save(newDetail);
    }

    @Test
    void createContactDetailAllowsNullDescription() {
        CourtContactDetails newDetail = CourtContactDetails.builder()
            .explanation("Phone enquiries")
            .email("info@test.com")
            .phoneNumber("09876543210")
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.save(any(CourtContactDetails.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtContactDetails result = courtContactDetailsService.createContactDetail(courtId, newDetail);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getCourtContactDescription()).isNull();
        verify(contactDescriptionTypeRepository, never()).findById(any());
    }

    @Test
    void createContactDetailThrowsWhenDescriptionNotFound() {
        UUID descriptionId = UUID.randomUUID();
        CourtContactDetails newDetail = CourtContactDetails.builder()
            .courtContactDescriptionId(descriptionId)
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(contactDescriptionTypeRepository.findById(descriptionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            courtContactDetailsService.createContactDetail(courtId, newDetail)
        );
        verify(courtContactDetailsRepository, never()).save(any());
    }

    @Test
    void updateContactDetailAppliesNewValues() {
        UUID descriptionId = UUID.randomUUID();
        ContactDescriptionType descriptionType = new ContactDescriptionType();
        descriptionType.setId(descriptionId);

        CourtContactDetails update = CourtContactDetails.builder()
            .courtContactDescriptionId(descriptionId)
            .explanation("Updated explanation")
            .explanationCy("Esboniad Diweddarwyd")
            .email("updated@test.com")
            .phoneNumber("09876543210")
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.findByIdAndCourtId(contactId, courtId))
            .thenReturn(Optional.of(contactDetail));
        when(contactDescriptionTypeRepository.findById(descriptionId)).thenReturn(Optional.of(descriptionType));
        when(courtContactDetailsRepository.save(contactDetail)).thenReturn(contactDetail);

        CourtContactDetails result = courtContactDetailsService.updateContactDetail(courtId, contactId, update);

        assertThat(result.getExplanation()).isEqualTo("Updated explanation");
        assertThat(result.getExplanationCy()).isEqualTo("Esboniad Diweddarwyd");
        assertThat(result.getEmail()).isEqualTo("updated@test.com");
        assertThat(result.getPhoneNumber()).isEqualTo("09876543210");
        assertThat(result.getCourtContactDescription()).isEqualTo(descriptionType);
    }

    @Test
    void updateContactDetailThrowsWhenNotFound() {
        CourtContactDetails update = CourtContactDetails.builder().build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.findByIdAndCourtId(contactId, courtId))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            courtContactDetailsService.updateContactDetail(courtId, contactId, update)
        );
    }

    @Test
    void deleteContactDetailRemovesEntry() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.existsByIdAndCourtId(contactId, courtId)).thenReturn(true);

        courtContactDetailsService.deleteContactDetail(courtId, contactId);

        verify(courtContactDetailsRepository).deleteByIdAndCourtId(contactId, courtId);
    }

    @Test
    void deleteContactDetailThrowsWhenMissing() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtContactDetailsRepository.existsByIdAndCourtId(contactId, courtId)).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
            courtContactDetailsService.deleteContactDetail(courtId, contactId)
        );
    }
}
