package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtDetailsViewServiceTest {

    @Mock
    private TypesService typesService;

    @InjectMocks
    private CourtDetailsViewService courtDetailsViewService;

    @Test
    void prepareDetailsViewReturnsNullForNullInput() {
        CourtDetails result = courtDetailsViewService.prepareDetailsView(null);

        assertThat(result).isNull();
        verifyNoInteractions(typesService);
    }

    @Test
    void prepareDetailsViewPopulatesOpeningHourTypes() {
        UUID openingHourTypeId = UUID.randomUUID();
        CourtOpeningHours openingHours = new CourtOpeningHours();
        openingHours.setOpeningHourTypeId(openingHourTypeId);

        OpeningHourType openingHourType = new OpeningHourType();
        openingHourType.setId(openingHourTypeId);
        openingHourType.setName("Counter service");

        when(typesService.getOpeningHourTypesByIds(List.of(openingHourTypeId)))
            .thenReturn(List.of(openingHourType));

        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setCourtOpeningHours(List.of(openingHours));

        courtDetailsViewService.prepareDetailsView(courtDetails);

        assertThat(openingHours.getOpeningHourTypeDetails()).isNotNull();
        assertThat(openingHours.getOpeningHourTypeDetails().getId()).isEqualTo(openingHourTypeId);
        assertThat(openingHours.getOpeningHourTypeDetails().getName()).isEqualTo("Counter service");
    }

    @Test
    void prepareDetailsViewFallsBackWhenOpeningHourTypeMissing() {
        UUID openingHourTypeId = UUID.randomUUID();
        CourtOpeningHours openingHours = new CourtOpeningHours();
        openingHours.setOpeningHourTypeId(openingHourTypeId);

        when(typesService.getOpeningHourTypesByIds(List.of(openingHourTypeId)))
            .thenReturn(List.of());

        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setCourtOpeningHours(List.of(openingHours));

        courtDetailsViewService.prepareDetailsView(courtDetails);

        assertThat(openingHours.getOpeningHourTypeDetails()).isNotNull();
        assertThat(openingHours.getOpeningHourTypeDetails().getId()).isEqualTo(openingHourTypeId);
        assertThat(openingHours.getOpeningHourTypeDetails().getName()).isNull();
    }

    @Test
    void prepareDetailsViewPopulatesContactDescriptions() {
        UUID descriptionId = UUID.randomUUID();
        CourtContactDetails contactDetails = new CourtContactDetails();
        contactDetails.setCourtContactDescriptionId(descriptionId);

        ContactDescriptionType descriptionType = new ContactDescriptionType();
        descriptionType.setId(descriptionId);
        descriptionType.setName("Enquiries");
        descriptionType.setNameCy("Ymholiadau");

        when(typesService.getContactDescriptionTypesByIds(List.of(descriptionId)))
            .thenReturn(List.of(descriptionType));

        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setCourtContactDetails(List.of(contactDetails));

        courtDetailsViewService.prepareDetailsView(courtDetails);

        assertThat(contactDetails.getCourtContactDescriptionDetails()).isNotNull();
        assertThat(contactDetails.getCourtContactDescriptionDetails().getId()).isEqualTo(descriptionId);
        assertThat(contactDetails.getCourtContactDescriptionDetails().getName()).isEqualTo("Enquiries");
        assertThat(contactDetails.getCourtContactDescriptionDetails().getNameCy()).isEqualTo("Ymholiadau");
    }

    @Test
    void prepareDetailsViewFallsBackWhenContactDescriptionMissing() {
        UUID descriptionId = UUID.randomUUID();
        CourtContactDetails contactDetails = new CourtContactDetails();
        contactDetails.setCourtContactDescriptionId(descriptionId);

        when(typesService.getContactDescriptionTypesByIds(List.of(descriptionId)))
            .thenReturn(List.of());

        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setCourtContactDetails(List.of(contactDetails));

        courtDetailsViewService.prepareDetailsView(courtDetails);

        assertThat(contactDetails.getCourtContactDescriptionDetails()).isNotNull();
        assertThat(contactDetails.getCourtContactDescriptionDetails().getId()).isEqualTo(descriptionId);
        assertThat(contactDetails.getCourtContactDescriptionDetails().getName()).isNull();
    }

    @Test
    void prepareDetailsViewPopulatesAddressTypesAndStubsMissing() {
        UUID areaId1 = UUID.randomUUID();
        UUID areaId2 = UUID.randomUUID();
        UUID courtTypeId1 = UUID.randomUUID();
        UUID courtTypeId2 = UUID.randomUUID();

        CourtAddress address = new CourtAddress();
        address.setAreasOfLaw(List.of(areaId1, areaId2));
        address.setCourtTypes(List.of(courtTypeId1, courtTypeId2));

        AreaOfLawType areaOfLawType = new AreaOfLawType();
        areaOfLawType.setId(areaId2);
        areaOfLawType.setName("Civil");

        CourtType courtType = new CourtType();
        courtType.setId(courtTypeId1);
        courtType.setName("Crown Court");

        when(typesService.getAllAreasOfLawTypesByIds(List.of(areaId1, areaId2)))
            .thenReturn(List.of(areaOfLawType));
        when(typesService.getAllCourtTypesByIds(List.of(courtTypeId1, courtTypeId2)))
            .thenReturn(List.of(courtType));

        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setCourtAddresses(List.of(address));

        courtDetailsViewService.prepareDetailsView(courtDetails);

        assertThat(address.getAreasOfLawDetails()).hasSize(2);
        assertThat(address.getAreasOfLawDetails().get(0).getId()).isEqualTo(areaId1);
        assertThat(address.getAreasOfLawDetails().get(0).getName()).isNull();
        assertThat(address.getAreasOfLawDetails().get(1).getId()).isEqualTo(areaId2);
        assertThat(address.getAreasOfLawDetails().get(1).getName()).isEqualTo("Civil");

        assertThat(address.getCourtTypeDetails()).hasSize(2);
        assertThat(address.getCourtTypeDetails().get(0).getId()).isEqualTo(courtTypeId1);
        assertThat(address.getCourtTypeDetails().get(0).getName()).isEqualTo("Crown Court");
        assertThat(address.getCourtTypeDetails().get(1).getId()).isEqualTo(courtTypeId2);
        assertThat(address.getCourtTypeDetails().get(1).getName()).isNull();
    }

    @Test
    void prepareDetailsViewPopulatesCourtAreasOfLawAndStubsMissing() {
        UUID areaId1 = UUID.randomUUID();
        UUID areaId2 = UUID.randomUUID();

        CourtAreasOfLaw courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setAreasOfLaw(List.of(areaId1, areaId2));

        AreaOfLawType areaOfLawType = new AreaOfLawType();
        areaOfLawType.setId(areaId2);
        areaOfLawType.setName("Divorce");

        when(typesService.getAllAreasOfLawTypesByIds(anyList()))
            .thenReturn(List.of(areaOfLawType));

        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setCourtAreasOfLaw(List.of(courtAreasOfLaw));

        courtDetailsViewService.prepareDetailsView(courtDetails);

        assertThat(courtAreasOfLaw.getAreasOfLawDetails()).hasSize(2);
        assertThat(courtAreasOfLaw.getAreasOfLawDetails().get(0).getId()).isEqualTo(areaId1);
        assertThat(courtAreasOfLaw.getAreasOfLawDetails().get(0).getName()).isNull();
        assertThat(courtAreasOfLaw.getAreasOfLawDetails().get(1).getId()).isEqualTo(areaId2);
        assertThat(courtAreasOfLaw.getAreasOfLawDetails().get(1).getName()).isEqualTo("Divorce");
    }
}
