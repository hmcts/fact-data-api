
package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeListDto;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeMoveDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPostcodeService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CourtPostcodeControllerTest {

    @Mock
    private CourtPostcodeService courtPostcodeService;

    @InjectMocks
    private CourtPostcodeController controller;

    @Captor
    private ArgumentCaptor<PostcodeListDto> postcodeListCaptor;

    private static final String COURT_ID_STR = UUID.randomUUID().toString();
    private static final UUID COURT_ID = UUID.fromString(COURT_ID_STR);

    private static CourtPostcode cp(UUID courtId, String postcode) {
        return CourtPostcode.builder().courtId(courtId).postcode(postcode).build();
    }

    @Nested
    class GetCourtPostcodesTests {

        @Test
        void returnsOkWithBodyWhenListNotEmpty() {
            List<CourtPostcode> list = List.of(
                cp(COURT_ID, "SW1 1AA"),
                cp(COURT_ID, "EC1 1BB")
            );
            when(courtPostcodeService.getPostcodesByCourtId(COURT_ID)).thenReturn(list);

            ResponseEntity<List<CourtPostcode>> response = controller.getCourtPostcodes(COURT_ID_STR);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsExactlyElementsOf(list);
            verify(courtPostcodeService).getPostcodesByCourtId(COURT_ID);
        }

        @Test
        void returnsNoContentWhenListEmpty() {
            when(courtPostcodeService.getPostcodesByCourtId(COURT_ID)).thenReturn(Collections.emptyList());

            ResponseEntity<List<CourtPostcode>> response = controller.getCourtPostcodes(COURT_ID_STR);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            assertThat(response.getBody()).isNull();
        }

        @Test
        void returnsNoContentWhenListNull() {
            when(courtPostcodeService.getPostcodesByCourtId(COURT_ID)).thenReturn(null);

            ResponseEntity<List<CourtPostcode>> response = controller.getCourtPostcodes(COURT_ID_STR);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            assertThat(response.getBody()).isNull();
        }

        @Test
        void propagatesNotFoundException() {
            when(courtPostcodeService.getPostcodesByCourtId(COURT_ID))
                .thenThrow(new NotFoundException("Court not found"));

            assertThrows(NotFoundException.class, () -> controller.getCourtPostcodes(COURT_ID_STR));
        }
    }

    @Nested
    class AddCourtPostcodesTests {

        @Test
        void returnsNoContentAndCallsService() {
            PostcodeListDto dto = mock(PostcodeListDto.class);

            ResponseEntity<Void> response = controller.addCourtPostcodes(COURT_ID_STR, dto);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(courtPostcodeService).addPostcodesToCourt(dto, COURT_ID);
        }

        @Test
        void propagatesDuplicatedListItemException() {
            PostcodeListDto dto = mock(PostcodeListDto.class);

            doThrow(new DuplicatedListItemException("Duplicated"))
                .when(courtPostcodeService).addPostcodesToCourt(any(PostcodeListDto.class), any(UUID.class));

            assertThrows(DuplicatedListItemException.class, () -> controller.addCourtPostcodes(COURT_ID_STR, dto));
        }

        @Test
        void propagatesNotFoundException() {
            PostcodeListDto dto = mock(PostcodeListDto.class);
            doThrow(new NotFoundException("Court not found"))
                .when(courtPostcodeService).addPostcodesToCourt(any(PostcodeListDto.class), any(UUID.class));

            assertThrows(NotFoundException.class, () -> controller.addCourtPostcodes(COURT_ID_STR, dto));
        }
    }

    @Nested
    class RemoveCourtPostcodesTests {

        @Test
        void returnsNoContentAndCallsService() {
            PostcodeListDto dto = mock(PostcodeListDto.class);

            ResponseEntity<Void> response = controller.removeCourtPostcodes(COURT_ID_STR, dto);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(courtPostcodeService).removePostcodesFromCourt(dto, COURT_ID);
        }

        @Test
        void propagatesNotFoundException() {
            PostcodeListDto dto = mock(PostcodeListDto.class);
            doThrow(new NotFoundException("Unassigned postcode"))
                .when(courtPostcodeService).removePostcodesFromCourt(any(PostcodeListDto.class), any(UUID.class));

            assertThrows(NotFoundException.class, () -> controller.removeCourtPostcodes(COURT_ID_STR, dto));
        }

        @Test
        void propagatesDuplicatedListItemException() {
            PostcodeListDto dto = mock(PostcodeListDto.class);
            doThrow(new DuplicatedListItemException("Duplicated"))
                .when(courtPostcodeService).removePostcodesFromCourt(any(PostcodeListDto.class), any(UUID.class));

            assertThrows(DuplicatedListItemException.class, () -> controller.removeCourtPostcodes(COURT_ID_STR, dto));
        }
    }

    @Nested
    class MigrateCourtPostcodesTests {

        @Test
        @DisplayName("returns 204 No Content and calls service")
        void returnsNoContentAndCallsService() {
            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);

            ResponseEntity<Void> response = controller.migrateCourtPostcodes(moveDto);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(courtPostcodeService).migratePostcodes(moveDto);
        }

        @Test
        void propagatesNotFoundException() {
            PostcodeMoveDto moveDto = mock(PostcodeMoveDto.class);
            doThrow(new NotFoundException("Court not found"))
                .when(courtPostcodeService).migratePostcodes(any(PostcodeMoveDto.class));

            assertThrows(NotFoundException.class, () -> controller.migrateCourtPostcodes(moveDto));
        }
    }
}
