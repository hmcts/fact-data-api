package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAreasOfLawRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreAreasOfLawServiceTest {

    @Mock
    private ServiceCentreAreasOfLawRepository serviceCentreAreasOfLawRepository;

    @Mock
    private ServiceCentreService serviceCentreService;

    @Mock
    private TypesService typesService;

    @InjectMocks
    private ServiceCentreAreasOfLawService serviceCentreAreasOfLawService;

    @Test
    void getServiceCentreAreasOfLawByServiceCentreIdReturnsRecord() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(serviceCentreId)
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAreasOfLawRepository.findByServiceCentreId(serviceCentreId))
            .thenReturn(Optional.of(areasOfLaw));

        assertThat(serviceCentreAreasOfLawService.getServiceCentreAreasOfLawByServiceCentreId(serviceCentreId))
            .isEqualTo(areasOfLaw);
    }

    @Test
    void getServiceCentreAreasOfLawByServiceCentreIdThrowsNotFoundWhenMissing() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAreasOfLawRepository.findByServiceCentreId(serviceCentreId)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> serviceCentreAreasOfLawService.getServiceCentreAreasOfLawByServiceCentreId(serviceCentreId)
        );
    }

    @Test
    void getAreasOfLawStatusReturnsAvailabilityMap() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID selectedAreaId = UUID.randomUUID();
        UUID unselectedAreaId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        AreaOfLawType selectedArea = new AreaOfLawType();
        selectedArea.setId(selectedAreaId);
        AreaOfLawType unselectedArea = new AreaOfLawType();
        unselectedArea.setId(unselectedAreaId);
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(serviceCentreId)
            .areasOfLaw(List.of(selectedAreaId))
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAreasOfLawRepository.findByServiceCentreId(serviceCentreId))
            .thenReturn(Optional.of(areasOfLaw));
        when(typesService.getAreaOfLawTypes()).thenReturn(List.of(selectedArea, unselectedArea));

        Map<AreaOfLawType, Boolean> result =
            serviceCentreAreasOfLawService.getAreasOfLawStatusByServiceCentreId(serviceCentreId);

        assertThat(result).containsEntry(selectedArea, true);
        assertThat(result).containsEntry(unselectedArea, false);
    }

    @Test
    void setServiceCentreAreasOfLawCreatesOrUpdatesRecordWithValidIds() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID existingRecordId = UUID.randomUUID();
        UUID suppliedAreaId = UUID.randomUUID();
        UUID validAreaId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).build();
        AreaOfLawType validArea = new AreaOfLawType();
        validArea.setId(validAreaId);
        ServiceCentreAreasOfLaw request = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(suppliedAreaId))
            .build();
        ServiceCentreAreasOfLaw existing = ServiceCentreAreasOfLaw.builder().id(existingRecordId).build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(typesService.getAllAreasOfLawTypesByIds(List.of(suppliedAreaId))).thenReturn(List.of(validArea));
        when(serviceCentreAreasOfLawRepository.findByServiceCentreId(serviceCentreId))
            .thenReturn(Optional.of(existing));
        when(serviceCentreAreasOfLawRepository.save(request)).thenReturn(request);

        ServiceCentreAreasOfLaw result =
            serviceCentreAreasOfLawService.setServiceCentreAreasOfLaw(serviceCentreId, request);

        assertThat(result.getId()).isEqualTo(existingRecordId);
        assertThat(result.getServiceCentreId()).isEqualTo(serviceCentreId);
        assertThat(result.getServiceCentre()).isEqualTo(serviceCentre);
        assertThat(result.getAreasOfLaw()).containsExactly(validAreaId);
        verify(serviceCentreAreasOfLawRepository).save(request);
    }
}
