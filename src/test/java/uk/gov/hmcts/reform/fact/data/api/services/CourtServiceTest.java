package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @InjectMocks
    private CourtService courtService;

    @Test
    void getCourtByIdReturnsCourtWhenFound() {
        UUID courtId = UUID.randomUUID();
        Court court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));

        Court result = courtService.getCourtById(courtId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(courtId);
        assertThat(result.getName()).isEqualTo("Test Court");
    }

    @Test
    void getCourtByIdThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        UUID courtId = UUID.randomUUID();

        when(courtRepository.findById(courtId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtService.getCourtById(courtId)
        );

        assertThat(exception.getMessage()).isEqualTo("Court not found, ID: " + courtId);
    }
}

