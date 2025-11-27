package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalAuthoritiesServiceTest {

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

    @InjectMocks
    private LocalAuthoritiesService localAuthoritiesService;

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
        when(localAuthorityTypeRepository.findAll()).thenReturn(List.of(laTwo, laOne));
        when(courtLocalAuthoritiesRepository.findByCourtIdAndAreaOfLawId(COURT_ID, ADOPTION_ID))
            .thenReturn(Optional.of(adoptionAuthorities));
        when(courtLocalAuthoritiesRepository.findByCourtIdAndAreaOfLawId(COURT_ID, CHILDREN_ID))
            .thenReturn(Optional.empty());

        List<CourtLocalAuthorityDto> result = localAuthoritiesService.getCourtLocalAuthorities(COURT_ID);

        assertThat(result).hasSize(2);
        CourtLocalAuthorityDto adoptionResult = findResultByAreaId(result, ADOPTION_ID);
        assertThat(adoptionResult.getLocalAuthorities()).hasSize(2);
        assertThat(adoptionResult.getLocalAuthorities().get(0).getName()).isEqualTo("Authority One");
        assertThat(adoptionResult.getLocalAuthorities().get(0).getSelected()).isTrue();

        CourtLocalAuthorityDto childrenResult = findResultByAreaId(result, CHILDREN_ID);
        assertThat(childrenResult.getLocalAuthorities()).allMatch(la -> Boolean.FALSE.equals(la.getSelected()));
    }

    @Test
    void shouldThrowWhenNoAreasOfLawConfiguredForCourt() {
        when(courtService.getCourtById(COURT_ID)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(COURT_ID)).thenReturn(Optional.empty());

        assertThrows(CourtResourceNotFoundException.class,
                     () -> localAuthoritiesService.getCourtLocalAuthorities(COURT_ID));
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

        localAuthoritiesService.setCourtLocalAuthorities(COURT_ID, updates);

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
            localAuthoritiesService.setCourtLocalAuthorities(COURT_ID, updates)
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
            localAuthoritiesService.setCourtLocalAuthorities(COURT_ID, updates)
        );

        verify(courtLocalAuthoritiesRepository, never()).deleteByCourtId(COURT_ID);
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
