
package uk.gov.hmcts.reform.fact.data.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidAreaOfLawException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CourtSinglePointsOfEntryService}.
 */
@ExtendWith(MockitoExtension.class)
class CourtSinglePointsOfEntryServiceTest {

    static AreaOfLawType adoption = AreaOfLawType.builder().id(UUID.randomUUID()).name("Adoption").build();
    static AreaOfLawType children = AreaOfLawType.builder().id(UUID.randomUUID()).name("Children").build();
    static AreaOfLawType civilPartnership = AreaOfLawType.builder().id(UUID.randomUUID()).name("Civil Partnership")
        .build();
    static AreaOfLawType divorce = AreaOfLawType.builder().id(UUID.randomUUID()).name("Divorce").build();
    static List<AreaOfLawType> allowedLocalAuthorityAreas = List.of(adoption, children, civilPartnership, divorce);
    static UUID courtId = UUID.randomUUID();

    @Mock
    private CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private AreaOfLawTypeRepository areaOfLawTypeRepository;

    @InjectMocks
    private CourtSinglePointsOfEntryService service;


    @Test
    void getShouldThrowNotFoundWhenCourtDoesNotExist() {
        when(courtRepository.existsById(courtId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCourtSinglePointsOfEntry(courtId));

        verify(courtRepository).existsById(courtId);
        verifyNoInteractions(courtSinglePointsOfEntryRepository, areaOfLawTypeRepository);
    }

    @Test
    void getShouldInitSelectionListWhenNoExistingConfig() {
        when(courtRepository.existsById(courtId)).thenReturn(true);
        when(courtSinglePointsOfEntryRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(areaOfLawTypeRepository.findByNameIn(anyList()))
            .thenReturn(allowedLocalAuthorityAreas);

        List<AreaOfLawSelectionDto> result = service.getCourtSinglePointsOfEntry(courtId);

        assertEquals(4, result.size());
        Map<UUID, Boolean> map = result.stream()
            .collect(Collectors.toMap(AreaOfLawSelectionDto::getId, AreaOfLawSelectionDto::getSelected));
        assertFalse(map.get(adoption.getId()));
        assertFalse(map.get(children.getId()));
        assertFalse(map.get(civilPartnership.getId()));
        assertFalse(map.get(divorce.getId()));

        verify(courtRepository).existsById(courtId);
        verify(courtSinglePointsOfEntryRepository).findByCourtId(courtId);
        verify(areaOfLawTypeRepository).findByNameIn(anyList());
    }

    @Test
    void getShouldMixSelectedAndUnselectedWhenConfigExists() {
        when(courtRepository.existsById(courtId)).thenReturn(true);

        // Existing SPOE config contains adoption selected
        CourtSinglePointsOfEntry existing = new CourtSinglePointsOfEntry();
        existing.setCourtId(courtId);
        existing.setAreasOfLaw(List.of(adoption.getId()));

        when(courtSinglePointsOfEntryRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));

        // configured areas
        when(areaOfLawTypeRepository.findAllById(existing.getAreasOfLaw()))
            .thenReturn(List.of(adoption));

        // Allowed areas
        when(areaOfLawTypeRepository.findByNameIn(anyList()))
            .thenReturn(allowedLocalAuthorityAreas);

        List<AreaOfLawSelectionDto> result = service.getCourtSinglePointsOfEntry(courtId);

        assertEquals(4, result.size());
        Map<UUID, Boolean> map = result.stream()
            .collect(Collectors.toMap(AreaOfLawSelectionDto::getId, AreaOfLawSelectionDto::getSelected));
        // this one was set
        assertTrue(map.get(adoption.getId()));
        // these weren't
        assertFalse(map.get(children.getId()));
        assertFalse(map.get(civilPartnership.getId()));
        assertFalse(map.get(divorce.getId()));

        verify(courtRepository).existsById(courtId);
        verify(courtSinglePointsOfEntryRepository).findByCourtId(courtId);
        verify(areaOfLawTypeRepository).findAllById(existing.getAreasOfLaw());
        verify(areaOfLawTypeRepository, times(2)).findByNameIn(anyList());
    }

    @Test
    void updateShouldThrowNotFoundWhenCourtDoesNotExist() {
        when(courtRepository.existsById(courtId)).thenReturn(false);

        List<AreaOfLawSelectionDto> updates = List.of(AreaOfLawSelectionDto.asSelected(adoption));
        assertThrows(NotFoundException.class, () -> service.updateCourtSinglePointsOfEntry(courtId, updates));

        verify(courtRepository).existsById(courtId);
        verifyNoInteractions(courtSinglePointsOfEntryRepository, areaOfLawTypeRepository);
    }

    @Test
    void updateShouldSaveOnlySelected() {
        when(courtRepository.existsById(courtId)).thenReturn(true);

        // Allowed areas
        when(areaOfLawTypeRepository.findByNameIn(anyList()))
            .thenReturn(allowedLocalAuthorityAreas);

        // Existing config
        CourtSinglePointsOfEntry existing = new CourtSinglePointsOfEntry();
        existing.setCourtId(courtId);
        existing.setAreasOfLaw(Collections.emptyList());
        when(courtSinglePointsOfEntryRepository.findByCourtId(courtId))
            .thenReturn(Optional.of(existing));

        // updates contain the whole set
        List<AreaOfLawSelectionDto> updates = List.of(
            AreaOfLawSelectionDto.asSelected(adoption),
            AreaOfLawSelectionDto.asUnselected(children),
            AreaOfLawSelectionDto.asUnselected(civilPartnership),
            AreaOfLawSelectionDto.asUnselected(divorce)
        );

        ArgumentCaptor<CourtSinglePointsOfEntry> captor = ArgumentCaptor.forClass(CourtSinglePointsOfEntry.class);

        service.updateCourtSinglePointsOfEntry(courtId, updates);

        verify(courtSinglePointsOfEntryRepository).save(captor.capture());
        CourtSinglePointsOfEntry saved = captor.getValue();

        assertEquals(courtId, saved.getCourtId());
        assertEquals(
            List.of(adoption.getId()), saved.getAreasOfLaw(),
            "Only selected areas should be persisted"
        );
    }

    @Test
    void updateShouldCreateNewEntityWhenNoConfigExists() {
        when(courtRepository.existsById(courtId)).thenReturn(true);

        // no existing config
        when(courtSinglePointsOfEntryRepository.findByCourtId(courtId))
            .thenReturn(Optional.empty());

        when(areaOfLawTypeRepository.findByNameIn(anyList()))
            .thenReturn(allowedLocalAuthorityAreas);

        // updates contain the whole set
        List<AreaOfLawSelectionDto> updates = List.of(
            AreaOfLawSelectionDto.asSelected(adoption),
            AreaOfLawSelectionDto.asUnselected(children),
            AreaOfLawSelectionDto.asSelected(civilPartnership),
            AreaOfLawSelectionDto.asUnselected(divorce)
        );

        ArgumentCaptor<CourtSinglePointsOfEntry> captor = ArgumentCaptor.forClass(CourtSinglePointsOfEntry.class);

        service.updateCourtSinglePointsOfEntry(courtId, updates);

        verify(courtSinglePointsOfEntryRepository).save(captor.capture());
        CourtSinglePointsOfEntry saved = captor.getValue();

        assertEquals(courtId, saved.getCourtId(), "New entity should carry the courtId");
        assertEquals(List.of(adoption.getId(), civilPartnership.getId()), saved.getAreasOfLaw());
    }

    @Test
    void updateShouldThrowDuplicatedListItemExceptionWhenDuplicateSelectedIdsPresent() {
        when(courtRepository.existsById(courtId)).thenReturn(true);

        // Duplicate selection: two selected DTOs with same ID
        List<AreaOfLawSelectionDto> updates = List.of(
            AreaOfLawSelectionDto.asSelected(adoption),
            AreaOfLawSelectionDto.asSelected(adoption),
            AreaOfLawSelectionDto.asUnselected(children),
            AreaOfLawSelectionDto.asSelected(civilPartnership),
            AreaOfLawSelectionDto.asUnselected(divorce)
        );

        assertThrows(DuplicatedListItemException.class, () -> service.updateCourtSinglePointsOfEntry(courtId, updates));

        verify(courtRepository).existsById(courtId);
        verify(courtSinglePointsOfEntryRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowInvalidAreaOfLawExceptionWhenSelectedContainsNotAllowed() {
        when(courtRepository.existsById(courtId)).thenReturn(true);

        AreaOfLawType probate = AreaOfLawType.builder().id(UUID.randomUUID()).name("Probate").build();

        // Allowed only area1; area3NotAllowed is not in allowed list
        when(areaOfLawTypeRepository.findByNameIn(anyList()))
            .thenReturn(allowedLocalAuthorityAreas);

        List<AreaOfLawSelectionDto> updates = List.of(
            AreaOfLawSelectionDto.asSelected(adoption),
            AreaOfLawSelectionDto.asUnselected(children),
            AreaOfLawSelectionDto.asSelected(civilPartnership),
            AreaOfLawSelectionDto.asUnselected(divorce),
            AreaOfLawSelectionDto.asSelected(probate)
        );

        assertThrows(InvalidAreaOfLawException.class, () -> service.updateCourtSinglePointsOfEntry(courtId, updates));

        verify(courtRepository).existsById(courtId);
        verify(areaOfLawTypeRepository).findByNameIn(anyList());
        verify(courtSinglePointsOfEntryRepository, never()).save(any());
    }
}
