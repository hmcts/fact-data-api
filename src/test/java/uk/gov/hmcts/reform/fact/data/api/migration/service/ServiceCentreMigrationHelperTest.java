package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
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
        assertThat(serviceCentreCaptor.getValue().getOpen()).isFalse();
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

    @Test
    void shouldReturnZeroWhenCourtListIsNull() {
        int migrated = helper.migrateServiceCentres(null, context);

        assertThat(migrated).isZero();
        verify(serviceCentreRepository, never()).save(any(ServiceCentre.class));
    }

    @Test
    void shouldSkipServiceCentreWhenSanitisedNameIsBlank() {
        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("123 !!!");

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isZero();
        verify(serviceCentreRepository, never()).save(any(ServiceCentre.class));
    }

    @Test
    void shouldSkipServiceCentreWhenNameIsNull() {
        CourtDto serviceCentreDto = buildMinimalServiceCentreDto(null);

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isZero();
        verify(serviceCentreRepository, never()).save(any(ServiceCentre.class));
    }

    @Test
    void shouldSkipServiceCentreWhenNameIsTooShortAfterSanitising() {
        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Abcd");

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isZero();
        verify(serviceCentreRepository, never()).save(any(ServiceCentre.class));
    }

    @Test
    void shouldSkipServiceCentreWhenNameIsTooLongAfterSanitising() {
        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("A".repeat(201));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isZero();
        verify(serviceCentreRepository, never()).save(any(ServiceCentre.class));
    }

    @Test
    void shouldPropagateValidationFailureWhenServiceCentreCannotBeSaved() {
        ConstraintViolationException exception = new ConstraintViolationException("invalid", Set.of());
        doThrow(exception).when(serviceCentreRepository).save(any(ServiceCentre.class));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");

        assertThatThrownBy(() -> helper.migrateServiceCentres(List.of(serviceCentreDto), context))
            .isSameAs(exception);
    }

    @Test
    void shouldPreserveCatchmentTypeWhenServiceAreaIdsAreUnmapped() {
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = new CourtDto();
        serviceCentreDto.setName("Legacy Service Centre");
        serviceCentreDto.setSlug("legacy-service-centre");
        serviceCentreDto.setOpen(true);
        serviceCentreDto.setIsServiceCentre(true);
        serviceCentreDto.setCourtServiceAreas(List.of(
            new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(999))
        ));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).isEmpty();
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isEqualTo(CatchmentType.LOCAL);
    }

    @Test
    void shouldMigrateServiceCentreWithoutServiceAreasOrAreasOfLaw() {
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);
        assertThat(context.getServiceCentreAreasOfLawMigrated()).isZero();

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).isEmpty();
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isNull();
        verify(serviceCentreAreasOfLawRepository, never()).save(any(ServiceCentreAreasOfLaw.class));
    }

    @Test
    void shouldSkipAreasOfLawWhenLegacyAreaIdsAreEmpty() {
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> {
            ServiceCentre serviceCentre = invocation.getArgument(0);
            serviceCentre.setId(UUID.randomUUID());
            return serviceCentre;
        });

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtAreasOfLaw(new CourtAreasOfLawDto("areas", List.of()));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);
        assertThat(context.getServiceCentreAreasOfLawMigrated()).isZero();
        verify(serviceCentreAreasOfLawRepository, never()).save(any(ServiceCentreAreasOfLaw.class));
    }

    @Test
    void shouldSkipAreasOfLawWhenAllAreaIdsAreUnmapped() {
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> {
            ServiceCentre serviceCentre = invocation.getArgument(0);
            serviceCentre.setId(UUID.randomUUID());
            return serviceCentre;
        });

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtAreasOfLaw(new CourtAreasOfLawDto("areas", List.of(999)));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);
        assertThat(context.getServiceCentreAreasOfLawMigrated()).isZero();
        verify(serviceCentreAreasOfLawRepository, never()).save(any(ServiceCentreAreasOfLaw.class));
    }

    @Test
    void shouldPreferRegionalCatchmentOverLocalWhenNationalIsAbsent() {
        UUID regionalServiceAreaId = UUID.randomUUID();
        UUID localServiceAreaId = UUID.randomUUID();
        context.getServiceAreaIds().put(1, localServiceAreaId);
        context.getServiceAreaIds().put(2, regionalServiceAreaId);
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(
            new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1)),
            new CourtServiceAreaDto(2, CatchmentType.REGIONAL.name(), List.of(2))
        ));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).containsExactly(regionalServiceAreaId);
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isEqualTo(CatchmentType.REGIONAL);
    }

    @Test
    void shouldChooseMappedServiceAreaWithoutCatchmentWhenOtherSelectionsAreUnmappedAndUnknown() {
        UUID serviceAreaId = UUID.randomUUID();
        context.getServiceAreaIds().put(1, serviceAreaId);
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(
            new CourtServiceAreaDto(1, null, List.of(1)),
            new CourtServiceAreaDto(2, "unknown", List.of(999))
        ));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).containsExactly(serviceAreaId);
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isNull();
    }

    @Test
    void shouldMapServiceAreasWithoutCatchmentType() {
        UUID serviceAreaId = UUID.randomUUID();
        context.getServiceAreaIds().put(1, serviceAreaId);
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(new CourtServiceAreaDto(1, null, List.of(1))));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).containsExactly(serviceAreaId);
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isNull();
    }

    @Test
    void shouldIgnoreServiceAreaSelectionWhenIdsAreEmptyAndCatchmentIsBlank() {
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(new CourtServiceAreaDto(1, " ", List.of())));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).isEmpty();
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isNull();
    }

    @Test
    void shouldSelectFirstMappedServiceAreaWhenCatchmentTypesAreAbsent() {
        UUID firstServiceAreaId = UUID.randomUUID();
        UUID secondServiceAreaId = UUID.randomUUID();
        context.getServiceAreaIds().put(1, firstServiceAreaId);
        context.getServiceAreaIds().put(2, secondServiceAreaId);
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(
            new CourtServiceAreaDto(1, null, List.of(1)),
            new CourtServiceAreaDto(2, null, List.of(2))
        ));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).containsExactly(firstServiceAreaId);
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isNull();
    }

    @Test
    void shouldMapServiceAreaIdsWhenCatchmentTypeIsUnknown() {
        UUID serviceAreaId = UUID.randomUUID();
        context.getServiceAreaIds().put(1, serviceAreaId);
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(new CourtServiceAreaDto(1, "unknown", List.of(1))));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).containsExactly(serviceAreaId);
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isNull();
    }

    @Test
    void shouldPreserveCatchmentTypeWhenLegacyServiceAreaIdsAreNull() {
        when(serviceCentreRepository.save(any(ServiceCentre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourtDto serviceCentreDto = buildMinimalServiceCentreDto("Legacy Service Centre");
        serviceCentreDto.setCourtServiceAreas(List.of(new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), null)));

        int migrated = helper.migrateServiceCentres(List.of(serviceCentreDto), context);

        assertThat(migrated).isEqualTo(1);

        ArgumentCaptor<ServiceCentre> serviceCentreCaptor = ArgumentCaptor.forClass(ServiceCentre.class);
        verify(serviceCentreRepository).save(serviceCentreCaptor.capture());
        assertThat(serviceCentreCaptor.getValue().getServiceAreaIds()).isEmpty();
        assertThat(serviceCentreCaptor.getValue().getCatchmentType()).isEqualTo(CatchmentType.LOCAL);
    }

    private CourtDto buildMinimalServiceCentreDto(String name) {
        CourtDto serviceCentreDto = new CourtDto();
        serviceCentreDto.setName(name);
        serviceCentreDto.setSlug("legacy-service-centre");
        serviceCentreDto.setOpen(true);
        serviceCentreDto.setIsServiceCentre(true);
        return serviceCentreDto;
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
