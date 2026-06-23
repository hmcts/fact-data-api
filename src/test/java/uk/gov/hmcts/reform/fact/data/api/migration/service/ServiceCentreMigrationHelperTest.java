package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

@ExtendWith(MockitoExtension.class)
class ServiceCentreMigrationHelperTest {

    @Mock private ServiceCentreRepository serviceCentreRepository;
    @Mock private ServiceCentreAreasOfLawRepository serviceCentreAreasOfLawRepository;

    private ServiceCentreMigrationHelper helper;
    private MigrationContext context;

    @BeforeEach
    void setUp() {
        helper = new ServiceCentreMigrationHelper(
            serviceCentreRepository,
            serviceCentreAreasOfLawRepository
        );
        context = new MigrationContext();
    }

    @Test
    void shouldPersistServiceCentreAndMappedChildRecords() {
        UUID serviceCentreId = UUID.randomUUID();
        UUID nationalServiceAreaId = UUID.randomUUID();
        UUID localServiceAreaId = UUID.randomUUID();
        UUID areaOfLawId = UUID.randomUUID();

        context.getServiceAreaIds().put(1, localServiceAreaId);
        context.getServiceAreaIds().put(2, nationalServiceAreaId);
        context.getAreaOfLawIds().put(10, areaOfLawId);

        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> {
            ServiceCentre serviceCentre = invocation.getArgument(0);
            serviceCentre.setId(serviceCentreId);
            return serviceCentre;
        });

        CourtDto serviceCentreDto = buildServiceCentreDto();

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);
        assertThat(context.getServiceCentreAreasOfLawMigrated()).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getName()).isEqualTo("Legacy Service Centre");
        assertThat(serviceCentreCaptor.getValue().getSlug()).isEqualTo("legacy-service-centre");
        assertThat(serviceCentreCaptor.getValue().getOpen()).isTrue();
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).containsExactly(nationalServiceAreaId);
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isEqualTo(CatchmentType.NATIONAL);

        ArgumentCaptor<ServiceCentreAreasOfLaw> areasCaptor =
            ArgumentCaptor.forClass(ServiceCentreAreasOfLaw.class);
        verify(serviceCentreAreasOfLawRepository).save(areasCaptor.capture());
        assertThat(areasCaptor.getValue().getServiceCentreId()).isEqualTo(serviceCentreId);
        assertThat(areasCaptor.getValue().getAreasOfLaw()).containsExactly(areaOfLawId);
    }

    @Test
    void shouldIgnoreCourtRows() {
        CourtDto courtDto = new CourtDto();
        courtDto.setIsServiceCentre(false);

        int migrated = helper.migrateServiceCentres(List.of(courtDto), context);

        assertThat(migrated).isZero();
        verify(serviceCentreRepository, never()).save(any(ServiceCentre.class));
    }

    private CourtDto buildServiceCentreDto() {
        CourtDto serviceCentreDto = new CourtDto();
        serviceCentreDto.setId(100L);
        serviceCentreDto.setName("Legacy Service Centre");
        serviceCentreDto.setSlug("legacy-service-centre");
        serviceCentreDto.setOpen(true);
        serviceCentreDto.setIsServiceCentre(true);
        serviceCentreDto.setCourtServiceAreas(List.of(
            new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1)),
            new CourtServiceAreaDto(2, CatchmentType.NATIONAL.name(), List.of(2))
        ));
        serviceCentreDto.setCourtAreasOfLaw(new CourtAreasOfLawDto("areas", List.of(10)));
        return serviceCentreDto;
    }
}
