package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtLocalAuthoritiesServiceTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID ADOPTION_ID = UUID.randomUUID();
    private static final UUID CHILDREN_ID = UUID.randomUUID();

    @Mock
    private CourtService courtService;

    @Mock
    private CourtAreasOfLawRepository courtAreasOfLawRepository;

    @Mock
    private CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;

    @Mock
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @Mock
    private AreaOfLawTypeRepository areaOfLawTypeRepository;

    @Mock
    private CourtCodesRepository courtCodesRepository;

    @InjectMocks
    private CourtLocalAuthoritiesService courtLocalAuthoritiesService;

    private AreaOfLawType adoption;
    private AreaOfLawType children;
    private LocalAuthorityType laOne;
    private LocalAuthorityType laTwo;
    private CourtAreasOfLaw courtAreasOfLaw;
    private uk.gov.hmcts.reform.fact.data.api.entities.Court court;

    @BeforeEach
    void setUp() {
        adoption = AreaOfLawType.builder().id(ADOPTION_ID).name("Adoption").build();
        children = AreaOfLawType.builder().id(CHILDREN_ID).name("Children").build();
        laOne = LocalAuthorityType.builder().id(UUID.randomUUID()).name("Authority One").build();
        laTwo = LocalAuthorityType.builder().id(UUID.randomUUID()).name("Authority Two").build();

        courtAreasOfLaw = CourtAreasOfLaw.builder()
            .id(UUID.randomUUID())
            .courtId(COURT_ID)
            .areasOfLaw(List.of(ADOPTION_ID, CHILDREN_ID))
            .build();

        court = new uk.gov.hmcts.reform.fact.data.api.entities.Court();
        court.setId(COURT_ID);
    }

    @Test
    void shouldReturnLocalAuthoritiesForAllowedAreas() {
        final CourtLocalAuthorities adoptionAuthorities = CourtLocalAuthorities.builder()
            .id(UUID.randomUUID())
            .courtId(COURT_ID)
            .areaOfLawId(ADOPTION_ID)
            .localAuthorityIds(List.of(laOne.getId()))
            .build();

        when(courtService.getCourtById(COURT_ID)).thenReturn(court);
        mockAllowedAreasOfLaw();
        when(localAuthorityTypeRepository.findAllParents()).thenReturn(List.of(laTwo, laOne));
        when(courtLocalAuthoritiesRepository.findByCourtIdAndAreaOfLawId(COURT_ID, ADOPTION_ID))
            .thenReturn(Optional.of(adoptionAuthorities));
        when(courtLocalAuthoritiesRepository.findByCourtIdAndAreaOfLawId(COURT_ID, CHILDREN_ID))
            .thenReturn(Optional.empty());

        List<CourtLocalAuthorityDto> result = courtLocalAuthoritiesService.getCourtLocalAuthorities(COURT_ID);

        assertThat(result).hasSize(2);
        CourtLocalAuthorityDto adoptionResult = findResultByAreaId(result, ADOPTION_ID);
        assertThat(adoptionResult.getLocalAuthorities()).hasSize(2);
        assertThat(adoptionResult.getLocalAuthorities().get(0).getName()).isEqualTo("Authority One");
        assertThat(adoptionResult.getLocalAuthorities().get(0).getSelected()).isTrue();

        CourtLocalAuthorityDto childrenResult = findResultByAreaId(result, CHILDREN_ID);
        List<LocalAuthoritySelectionDto> childrenAuthorities = childrenResult.getLocalAuthorities();
        assertThat(childrenAuthorities).isNotEmpty();
        assertThat(childrenAuthorities).allMatch(la -> Boolean.FALSE.equals(la.getSelected()));
        verify(localAuthorityTypeRepository).findAllParents();
        verify(localAuthorityTypeRepository, never()).findAll();
    }

    @Test
    void shouldThrowWhenNoAreasOfLawConfiguredForCourt() {
        when(courtService.getCourtById(COURT_ID)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(COURT_ID)).thenReturn(Optional.empty());

        assertThrows(CourtResourceNotFoundException.class,
                     () -> courtLocalAuthoritiesService.getCourtLocalAuthorities(COURT_ID));
    }

    @Test
    void shouldSetCourtLocalAuthoritiesForAllowedAreas() {
        when(courtService.getCourtById(COURT_ID)).thenReturn(court);
        mockAllowedAreasOfLaw();

        List<CourtLocalAuthorityDto> updates = List.of(
            CourtLocalAuthorityDto.builder()
                .areaOfLawId(ADOPTION_ID)
                .localAuthorities(List.of(
                    LocalAuthoritySelectionDto.builder()
                        .id(laOne.getId())
                        .name("Authority One")
                        .selected(true)
                        .build(),
                    LocalAuthoritySelectionDto.builder()
                        .id(laTwo.getId())
                        .name("Authority Two")
                        .selected(false)
                        .build()))
                .build(),
            CourtLocalAuthorityDto.builder()
                .areaOfLawId(CHILDREN_ID)
                .localAuthorities(List.of(
                    LocalAuthoritySelectionDto.builder()
                        .id(laOne.getId())
                        .name("Authority One")
                        .selected(false)
                        .build()))
                .build()
        );

        courtLocalAuthoritiesService.setCourtLocalAuthorities(COURT_ID, updates);

        verify(courtLocalAuthoritiesRepository).deleteByCourtId(COURT_ID);
        verify(courtLocalAuthoritiesRepository).saveAll(argThat(
            (Iterable<CourtLocalAuthorities> saved) -> {
                List<CourtLocalAuthorities> entries =
                    StreamSupport.stream(saved.spliterator(), false).toList();

                if (entries.size() != 2) {
                    return false;
                }

                CourtLocalAuthorities adoptionSave = findSavedByAreaId(entries, ADOPTION_ID);
                CourtLocalAuthorities childrenSave = findSavedByAreaId(entries, CHILDREN_ID);

                return adoptionSave.getCourtId().equals(COURT_ID)
                    && adoptionSave.getLocalAuthorityIds().equals(List.of(laOne.getId()))
                    && childrenSave.getCourtId().equals(COURT_ID)
                    && childrenSave.getLocalAuthorityIds().isEmpty();
            }
        ));
    }

    @Test
    void shouldThrowWhenMissingAllowedAreaUpdate() {
        when(courtService.getCourtById(COURT_ID)).thenReturn(court);
        mockAllowedAreasOfLaw();

        List<CourtLocalAuthorityDto> updates = List.of(
            CourtLocalAuthorityDto.builder()
                .areaOfLawId(ADOPTION_ID)
                .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                    .id(laOne.getId())
                    .selected(true)
                    .build()))
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            courtLocalAuthoritiesService.setCourtLocalAuthorities(COURT_ID, updates)
        );

        verify(courtLocalAuthoritiesRepository, never()).deleteByCourtId(COURT_ID);
    }

    @Test
    void shouldThrowWhenDuplicateAreaOfLawIdsProvided() {
        when(courtService.getCourtById(COURT_ID)).thenReturn(court);
        mockAllowedAreasOfLaw();

        List<CourtLocalAuthorityDto> updates = List.of(
            CourtLocalAuthorityDto.builder()
                .areaOfLawId(ADOPTION_ID)
                .localAuthorities(List.of())
                .build(),
            CourtLocalAuthorityDto.builder()
                .areaOfLawId(ADOPTION_ID)
                .localAuthorities(List.of())
                .build()
        );

        assertThrows(IllegalStateException.class, () ->
            courtLocalAuthoritiesService.setCourtLocalAuthorities(COURT_ID, updates)
        );

        verify(courtLocalAuthoritiesRepository, never()).deleteByCourtId(COURT_ID);
    }

    @Test
    void housekeepingShouldRetainOnlyAllowedAreasForFamilyCourts() {
        UUID disallowedAreaId = UUID.randomUUID();

        when(courtCodesRepository.findByCourtId(COURT_ID))
            .thenReturn(Optional.of(CourtCodes.builder().courtId(COURT_ID).familyCourtCode(12345).build()));
        when(courtAreasOfLawRepository.findByCourtId(COURT_ID))
            .thenReturn(Optional.of(CourtAreasOfLaw.builder()
                                .courtId(COURT_ID)
                                .areasOfLaw(List.of(ADOPTION_ID, disallowedAreaId))
                                .build()));
        when(areaOfLawTypeRepository.findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames()))
            .thenReturn(List.of(adoption, children));

        courtLocalAuthoritiesService.performHousekeeping(COURT_ID);

        verify(courtLocalAuthoritiesRepository)
            .deleteByCourtIdAndAreaOfLawIdNotIn(COURT_ID, List.of(ADOPTION_ID));
    }

    @Test
    void housekeepingShouldPruneAllForNonFamilyCourts() {
        when(courtCodesRepository.findByCourtId(COURT_ID))
            .thenReturn(Optional.of(CourtCodes.builder().courtId(COURT_ID).familyCourtCode(null).build()));

        courtLocalAuthoritiesService.performHousekeeping(COURT_ID);

        verify(courtLocalAuthoritiesRepository)
            .deleteByCourtIdAndAreaOfLawIdNotIn(COURT_ID, List.of());
        verify(courtAreasOfLawRepository, never()).findByCourtId(COURT_ID);
        verify(areaOfLawTypeRepository, never()).findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames());
    }

    @Test
    void housekeepingShouldNotThrowIfRepositoryFails() {
        when(courtCodesRepository.findByCourtId(COURT_ID)).thenThrow(new RuntimeException("database unavailable"));

        assertDoesNotThrow(() -> courtLocalAuthoritiesService.performHousekeeping(COURT_ID));

        verify(courtLocalAuthoritiesRepository, never()).deleteByCourtIdAndAreaOfLawIdNotIn(COURT_ID, List.of());
    }

    private void mockAllowedAreasOfLaw() {
        when(areaOfLawTypeRepository.findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames()))
            .thenReturn(List.of(adoption, children));
        when(courtAreasOfLawRepository.findByCourtId(COURT_ID)).thenReturn(Optional.of(courtAreasOfLaw));
        when(areaOfLawTypeRepository.findAllById(courtAreasOfLaw.getAreasOfLaw()))
            .thenReturn(List.of(adoption, children));
    }

    private CourtLocalAuthorityDto findResultByAreaId(List<CourtLocalAuthorityDto> results, UUID areaId) {
        return results.stream()
            .filter(result -> areaId.equals(result.getAreaOfLawId()))
            .findFirst()
            .orElseThrow();
    }

    private CourtLocalAuthorities findSavedByAreaId(List<CourtLocalAuthorities> saved, UUID areaId) {
        return saved.stream()
            .filter(entry -> areaId.equals(entry.getAreaOfLawId()))
            .findFirst()
            .orElseThrow();
    }
}
