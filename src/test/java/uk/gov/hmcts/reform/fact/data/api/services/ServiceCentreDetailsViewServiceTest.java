package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreDetailsViewServiceTest {

    @Mock
    private TypesService typesService;

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private ServiceCentreDetailsViewService serviceCentreDetailsViewService;

    @Test
    void prepareDetailsViewReturnsNullForNullInput() {
        ServiceCentreDetails result = serviceCentreDetailsViewService.prepareDetailsView(null);

        assertThat(result).isNull();
        verifyNoInteractions(typesService);
        verifyNoInteractions(serviceAreaRepository);
    }

    @Test
    void prepareDetailsViewDoesNotCallDependenciesWhenChildCollectionsAreEmpty() {
        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .serviceCentreContactDetails(List.of())
            .serviceCentreAreasOfLaw(List.of())
            .build();

        ServiceCentreDetails result = serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails);

        assertThat(result).isSameAs(serviceCentreDetails);
        verifyNoInteractions(typesService);
        verifyNoInteractions(serviceAreaRepository);
    }

    @Test
    void prepareDetailsViewPopulatesServiceAreasAndStubsMissing() {
        UUID serviceAreaId1 = UUID.randomUUID();
        UUID serviceAreaId2 = UUID.randomUUID();

        ServiceArea serviceArea = ServiceArea.builder()
            .id(serviceAreaId2)
            .name("Family")
            .nameCy("Teulu")
            .build();

        when(serviceAreaRepository.findAllById(List.of(serviceAreaId1, serviceAreaId2)))
            .thenReturn(List.of(serviceArea));

        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .serviceAreaIds(List.of(serviceAreaId1, serviceAreaId2))
            .build();

        serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails);

        assertThat(serviceCentreDetails.getServiceAreaDetails()).hasSize(2);
        assertThat(serviceCentreDetails.getServiceAreaDetails().get(0).getId()).isEqualTo(serviceAreaId1);
        assertThat(serviceCentreDetails.getServiceAreaDetails().get(0).getName()).isNull();
        assertThat(serviceCentreDetails.getServiceAreaDetails().get(1).getId()).isEqualTo(serviceAreaId2);
        assertThat(serviceCentreDetails.getServiceAreaDetails().get(1).getName()).isEqualTo("Family");
        assertThat(serviceCentreDetails.getServiceAreaDetails().get(1).getNameCy()).isEqualTo("Teulu");
    }

    @Test
    void prepareDetailsViewPopulatesContactDescriptions() {
        UUID descriptionId = UUID.randomUUID();
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(descriptionId)
            .build();

        ContactDescriptionType descriptionType = ContactDescriptionType.builder()
            .id(descriptionId)
            .name("Enquiries")
            .nameCy("Ymholiadau")
            .build();

        when(typesService.getContactDescriptionTypesByIds(List.of(descriptionId)))
            .thenReturn(List.of(descriptionType));

        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .serviceCentreContactDetails(List.of(contactDetails))
            .build();

        serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails);

        assertThat(contactDetails.getServiceCentreContactDescriptionDetails()).isNotNull();
        assertThat(contactDetails.getServiceCentreContactDescriptionDetails().getId()).isEqualTo(descriptionId);
        assertThat(contactDetails.getServiceCentreContactDescriptionDetails().getName()).isEqualTo("Enquiries");
        assertThat(contactDetails.getServiceCentreContactDescriptionDetails().getNameCy()).isEqualTo("Ymholiadau");
    }

    @Test
    void prepareDetailsViewFallsBackWhenContactDescriptionMissing() {
        UUID descriptionId = UUID.randomUUID();
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(descriptionId)
            .build();

        when(typesService.getContactDescriptionTypesByIds(List.of(descriptionId)))
            .thenReturn(List.of());

        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .serviceCentreContactDetails(List.of(contactDetails))
            .build();

        serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails);

        assertThat(contactDetails.getServiceCentreContactDescriptionDetails()).isNotNull();
        assertThat(contactDetails.getServiceCentreContactDescriptionDetails().getId()).isEqualTo(descriptionId);
        assertThat(contactDetails.getServiceCentreContactDescriptionDetails().getName()).isNull();
        assertThat(contactDetails.getServiceCentreContactDescriptionDetails().getNameCy()).isNull();
    }

    @Test
    void prepareDetailsViewPopulatesAreasOfLawAndStubsMissing() {
        UUID areaId1 = UUID.randomUUID();
        UUID areaId2 = UUID.randomUUID();
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(areaId1, areaId2))
            .build();

        AreaOfLawType areaOfLawType = AreaOfLawType.builder()
            .id(areaId2)
            .name("Civil")
            .nameCy("Sifil")
            .build();

        when(typesService.getAllAreasOfLawTypesByIds(List.of(areaId1, areaId2)))
            .thenReturn(List.of(areaOfLawType));

        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .serviceCentreAreasOfLaw(List.of(areasOfLaw))
            .build();

        serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails);

        assertThat(areasOfLaw.getAreasOfLawDetails()).hasSize(2);
        assertThat(areasOfLaw.getAreasOfLawDetails().get(0).getId()).isEqualTo(areaId1);
        assertThat(areasOfLaw.getAreasOfLawDetails().get(0).getName()).isNull();
        assertThat(areasOfLaw.getAreasOfLawDetails().get(1).getId()).isEqualTo(areaId2);
        assertThat(areasOfLaw.getAreasOfLawDetails().get(1).getName()).isEqualTo("Civil");
        assertThat(areasOfLaw.getAreasOfLawDetails().get(1).getNameCy()).isEqualTo("Sifil");
    }

    @Test
    void prepareDetailsViewUsesDistinctTypeIdsAcrossChildCollections() {
        UUID descriptionId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();

        ServiceCentreContactDetails contactDetails1 = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(descriptionId)
            .build();
        ServiceCentreContactDetails contactDetails2 = ServiceCentreContactDetails.builder()
            .serviceCentreContactDescriptionId(descriptionId)
            .build();
        ServiceCentreAreasOfLaw areasOfLaw1 = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(areaId))
            .build();
        ServiceCentreAreasOfLaw areasOfLaw2 = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(areaId))
            .build();

        ContactDescriptionType descriptionType = ContactDescriptionType.builder()
            .id(descriptionId)
            .name("Telephone")
            .build();
        AreaOfLawType areaOfLawType = AreaOfLawType.builder()
            .id(areaId)
            .name("Family")
            .build();

        when(typesService.getContactDescriptionTypesByIds(List.of(descriptionId)))
            .thenReturn(List.of(descriptionType));
        when(typesService.getAllAreasOfLawTypesByIds(List.of(areaId)))
            .thenReturn(List.of(areaOfLawType));

        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .serviceCentreContactDetails(List.of(contactDetails1, contactDetails2))
            .serviceCentreAreasOfLaw(List.of(areasOfLaw1, areasOfLaw2))
            .build();

        serviceCentreDetailsViewService.prepareDetailsView(serviceCentreDetails);

        assertThat(contactDetails1.getServiceCentreContactDescriptionDetails()).isSameAs(descriptionType);
        assertThat(contactDetails2.getServiceCentreContactDescriptionDetails()).isSameAs(descriptionType);
        assertThat(areasOfLaw1.getAreasOfLawDetails()).containsExactly(areaOfLawType);
        assertThat(areasOfLaw2.getAreasOfLawDetails()).containsExactly(areaOfLawType);
    }
}
