
package uk.gov.hmcts.reform.fact.data.api.services;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeMigrationRequestException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeListDto;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeMoveDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPostcodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtPostcodeServiceTest {

    @Mock
    private CourtPostcodeRepository courtPostcodeRepository;

    @Mock
    private CourtRepository courtRepository;

    @InjectMocks
    private CourtPostcodeService service;

    @Captor
    private ArgumentCaptor<List<CourtPostcode>> courtPostcodeListCaptor;

    @Captor
    private ArgumentCaptor<List<String>> stringListCaptor;

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID SOURCE_COURT_ID = UUID.randomUUID();
    private static final UUID DEST_COURT_ID = UUID.randomUUID();

    private static CourtPostcode buildCourtPostcode(UUID courtId, String postcode) {
        return CourtPostcode.builder().courtId(courtId).postcode(postcode).build();
    }

    @Nested
    class GetCourtPostcodes {
        @Test
        void getPostcodesByCourtIdReturnsListWhenCourtExists() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);
            List<CourtPostcode> expected = List.of(
                buildCourtPostcode(COURT_ID, "SW1 1AA"),
                buildCourtPostcode(COURT_ID, "EC1 1BB")
            );
            when(courtPostcodeRepository.getCourtPostcodeByCourtId(COURT_ID)).thenReturn(expected);

            List<CourtPostcode> result = service.getPostcodesByCourtId(COURT_ID);

            assertThat(result).containsExactlyElementsOf(expected);
            verify(courtRepository).existsById(COURT_ID);
            verify(courtPostcodeRepository).getCourtPostcodeByCourtId(COURT_ID);
        }

        @Test
        void getPostcodesByCourtIdThrowsNotFoundWhenCourtMissing() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> service.getPostcodesByCourtId(COURT_ID));

            verify(courtRepository).existsById(COURT_ID);
            verifyNoInteractions(courtPostcodeRepository);
        }
    }

    @Nested
    class AddCourtPostcodes {
        @Test
        void addPostcodesToCourtSavesOnlyNewAndNormalises() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);

            // Input postcodes (one already exists when normalised)
            PostcodeListDto dto = mock(PostcodeListDto.class);
            when(dto.getPostcodes()).thenReturn(asList("sw11aa", "EC11BB", "hp70", "IP9"));

            // Existing on this court (note: already normalised "SW1A 1AA")
            when(courtPostcodeRepository.findAllByCourtId(COURT_ID)).thenReturn(
                List.of(buildCourtPostcode(COURT_ID, "SW11AA"))
            );

            service.addPostcodesToCourt(dto, COURT_ID);

            // saveAll called with only the *new* one, normalised to "EC1A 1BB"
            verify(courtPostcodeRepository).saveAll(courtPostcodeListCaptor.capture());
            List<CourtPostcode> saved = courtPostcodeListCaptor.getValue();
            assertThat(saved).hasSize(3);
            assertThat(saved.getFirst().getCourtId()).isEqualTo(COURT_ID);
            assertThat(saved.getFirst().getPostcode()).isEqualTo("EC11BB");
            // ensure partial worked
            assertThat(saved.get(1).getCourtId()).isEqualTo(COURT_ID);
            assertThat(saved.get(1).getPostcode()).isEqualTo("HP70");
            assertThat(saved.getLast().getCourtId()).isEqualTo(COURT_ID);
            assertThat(saved.getLast().getPostcode()).isEqualTo("IP9");

        }


        @Test
        void addPostcodesToCourtDoesNotSaveWhenNoNew() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);

            PostcodeListDto dto = mock(PostcodeListDto.class);
            when(dto.getPostcodes()).thenReturn(List.of("SW1 1AA"));

            // Already exists
            when(courtPostcodeRepository.findAllByCourtId(COURT_ID)).thenReturn(
                List.of(buildCourtPostcode(COURT_ID, "SW11AA"))
            );

            service.addPostcodesToCourt(dto, COURT_ID);

            verify(courtPostcodeRepository, never()).saveAll(anyList());
        }

        @Test
        void addPostcodesToCourtThrowsDuplicatedListItemForDuplicatedPostcodes() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);

            PostcodeListDto dto = mock(PostcodeListDto.class);
            when(dto.getPostcodes()).thenReturn(asList("SW1 1AA", "sw11aa"));

            assertThrows(DuplicatedListItemException.class, () -> service.addPostcodesToCourt(dto, COURT_ID));

            verify(courtPostcodeRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    class DeleteCourtPostcodes {
        @Test
        void removePostcodesFromCourtDeletesNormalisedPostcodesWhenAllExist() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);

            PostcodeListDto dto = mock(PostcodeListDto.class);
            // Note: lower-case & no space -> should normalise to ["SW1A 1AA","EC1A 1BB"]
            List<String> input = asList("sw11aa", "EC11BB");
            when(dto.getPostcodes()).thenReturn(input);

            List<CourtPostcode> toDelete = List.of(
                buildCourtPostcode(COURT_ID, "SW1 1AA"),
                buildCourtPostcode(COURT_ID, "EC1 1BB")
            );

            when(courtPostcodeRepository.findAllByCourtIdAndPostcodeIn(eq(COURT_ID), anyList()))
                .thenReturn(toDelete);

            service.removePostcodesFromCourt(dto, COURT_ID);

            // Verify the query was made with normalised postcodes
            verify(courtPostcodeRepository).findAllByCourtIdAndPostcodeIn(eq(COURT_ID), stringListCaptor.capture());
            List<String> normalisedList = stringListCaptor.getValue();
            assertThat(normalisedList).containsExactly("SW11AA", "EC11BB");

            // And verify deletion
            verify(courtPostcodeRepository).deleteAll(toDelete);
        }

        @Test
        void removePostcodesFromCourtThrowsNotFoundWhenAnyPostcodesMissing() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);

            PostcodeListDto dto = mock(PostcodeListDto.class);
            when(dto.getPostcodes()).thenReturn(asList("SW1 1AA", "EC1 1BB"));

            // Pretend only one of the two exists on this court
            when(courtPostcodeRepository.findAllByCourtIdAndPostcodeIn(eq(COURT_ID), anyList()))
                .thenReturn(List.of(buildCourtPostcode(COURT_ID, "SW11AA")));

            NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> service.removePostcodesFromCourt(dto, COURT_ID)
            );

            assertThat(ex.getMessage())
                .contains("Unassigned postcode(s) in delete request")
                .contains("EC11BB")
                .contains(COURT_ID.toString());

            verify(courtPostcodeRepository, never()).deleteAll(anyList());
        }

        @Test
        void removePostcodesFromCourtDoesNothingWhenEmptyList() {
            when(courtRepository.existsById(COURT_ID)).thenReturn(true);

            PostcodeListDto dto = mock(PostcodeListDto.class);
            when(dto.getPostcodes()).thenReturn(List.of());

            service.removePostcodesFromCourt(dto, COURT_ID);

            verify(courtPostcodeRepository, never()).deleteAll(anyList());
            verify(courtPostcodeRepository, never()).findAllByCourtIdAndPostcodeIn(any(), anyList());
        }
    }

    @Nested
    class MigrateCourtPostcodes {
        @Test
        void migratePostcodesWorksForAValidRequest() {
            when(courtRepository.existsById(any(UUID.class))).thenReturn(true);

            PostcodeListDto listDto = mock(PostcodeListDto.class);
            // EC1 1BB already on destination; SW1 1AA and W1 0AX are new
            when(listDto.getPostcodes()).thenReturn(asList("sw11AA", "EC11BB", "W1 0AX"));

            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            when(moveDto.getSourceCourtId()).thenReturn(SOURCE_COURT_ID);
            when(moveDto.getDestinationCourtId()).thenReturn(DEST_COURT_ID);
            when(moveDto.getPostcodeList()).thenReturn(listDto);

            // For deletion from source: all three exist on source, after normalisation
            List<CourtPostcode> sourceExisting = List.of(
                buildCourtPostcode(SOURCE_COURT_ID, "SW11AA"),
                buildCourtPostcode(SOURCE_COURT_ID, "EC11BB"),
                buildCourtPostcode(SOURCE_COURT_ID, "W10AX")
            );
            when(courtPostcodeRepository.findAllByCourtIdAndPostcodeIn(eq(SOURCE_COURT_ID), anyList()))
                .thenReturn(sourceExisting);

            // For adding to destination: one already exists (EC1 1BB), the rest are new
            when(courtPostcodeRepository.findAllByCourtId(DEST_COURT_ID))
                .thenReturn(List.of(buildCourtPostcode(DEST_COURT_ID, "EC11BB")));

            service.migratePostcodes(moveDto);

            // Verify deletes on source
            verify(courtPostcodeRepository).deleteAll(sourceExisting);

            // Verify saveAll with only the two that are not on destination
            verify(courtPostcodeRepository).saveAll(courtPostcodeListCaptor.capture());
            List<CourtPostcode> savedToDest = courtPostcodeListCaptor.getValue();
            assertThat(savedToDest).hasSize(2);
            assertThat(savedToDest)
                .extracting(CourtPostcode::getPostcode)
                .containsExactlyInAnyOrder("SW11AA", "W10AX");
            assertThat(savedToDest)
                .allMatch(cp -> DEST_COURT_ID.equals(cp.getCourtId()));
        }

        @Test
        void migratePostcodesNoSavesWhenNothingNewToAdd() {
            when(courtRepository.existsById(any(UUID.class))).thenReturn(true);

            PostcodeListDto listDto = mock(PostcodeListDto.class);
            when(listDto.getPostcodes()).thenReturn(asList("SW1 1AA", "EC1 1BB"));

            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            when(moveDto.getSourceCourtId()).thenReturn(SOURCE_COURT_ID);
            when(moveDto.getDestinationCourtId()).thenReturn(DEST_COURT_ID);
            when(moveDto.getPostcodeList()).thenReturn(listDto);

            // Deletion side: both exist on source
            when(courtPostcodeRepository.findAllByCourtIdAndPostcodeIn(eq(SOURCE_COURT_ID), anyList()))
                .thenReturn(List.of(
                    buildCourtPostcode(SOURCE_COURT_ID, "SW11AA"),
                    buildCourtPostcode(SOURCE_COURT_ID, "EC11BB")
                ));

            // Destination already has both
            when(courtPostcodeRepository.findAllByCourtId(DEST_COURT_ID))
                .thenReturn(List.of(
                    buildCourtPostcode(DEST_COURT_ID, "SW11AA"),
                    buildCourtPostcode(DEST_COURT_ID, "EC11BB")
                ));

            service.migratePostcodes(moveDto);

            verify(courtPostcodeRepository).deleteAll(anyList());
            verify(courtPostcodeRepository, never()).saveAll(anyList());
        }

        @Test
        void migratePostcodesThrowsInvalidPostcodeMigrationRequestWhenSameCourtIds() {
            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            when(moveDto.getSourceCourtId()).thenReturn(COURT_ID);
            when(moveDto.getDestinationCourtId()).thenReturn(COURT_ID);

            assertThrows(InvalidPostcodeMigrationRequestException.class, () -> service.migratePostcodes(moveDto));

            verifyNoInteractions(courtPostcodeRepository);
        }

        @Test
        void migratePostcodesThrowsNotFoundWhenCourtMissing() {
            // Source exists, destination missing
            when(courtRepository.existsById(SOURCE_COURT_ID)).thenReturn(true);
            when(courtRepository.existsById(DEST_COURT_ID)).thenReturn(false);

            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            when(moveDto.getSourceCourtId()).thenReturn(SOURCE_COURT_ID);
            when(moveDto.getDestinationCourtId()).thenReturn(DEST_COURT_ID);

            // We don't need to stub getPostcodeList; it should fail before that
            assertThrows(NotFoundException.class, () -> service.migratePostcodes(moveDto));

            verifyNoInteractions(courtPostcodeRepository);
        }

        @Test
        void migratePostcodesThrowsDuplicatedListItemOnDuplicateList() {
            when(courtRepository.existsById(any(UUID.class))).thenReturn(true);

            PostcodeListDto listDto = mock(PostcodeListDto.class);
            when(listDto.getPostcodes()).thenReturn(asList("SW1 1AA", "sw11aa"));

            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            when(moveDto.getSourceCourtId()).thenReturn(SOURCE_COURT_ID);
            when(moveDto.getDestinationCourtId()).thenReturn(DEST_COURT_ID);
            when(moveDto.getPostcodeList()).thenReturn(listDto);

            assertThrows(DuplicatedListItemException.class, () -> service.migratePostcodes(moveDto));

            verifyNoInteractions(courtPostcodeRepository);
        }

        @Test
        void migratePostcodesThrowsNotFoundWhenSourceMissingSomePostcodes() {
            when(courtRepository.existsById(any(UUID.class))).thenReturn(true);

            PostcodeListDto listDto = mock(PostcodeListDto.class);
            when(listDto.getPostcodes()).thenReturn(asList("SW1 1AA", "EC1 1BB"));

            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            when(moveDto.getSourceCourtId()).thenReturn(SOURCE_COURT_ID);
            when(moveDto.getDestinationCourtId()).thenReturn(DEST_COURT_ID);
            when(moveDto.getPostcodeList()).thenReturn(listDto);

            // Only one exists on source; createNormalisedPostcodesToDeleteList should throw NotFoundException
            when(courtPostcodeRepository.findAllByCourtIdAndPostcodeIn(eq(SOURCE_COURT_ID), anyList()))
                .thenReturn(List.of(buildCourtPostcode(SOURCE_COURT_ID, "SW11AA")));

            assertThrows(NotFoundException.class, () -> service.migratePostcodes(moveDto));

            verify(courtPostcodeRepository, never()).deleteAll(anyList());
            verify(courtPostcodeRepository, never()).saveAll(anyList());
        }
    }
}
