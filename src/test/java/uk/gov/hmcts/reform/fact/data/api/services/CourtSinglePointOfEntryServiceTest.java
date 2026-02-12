package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtSinglePointOfEntryServiceTest {

    @Mock
    private CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;

    @InjectMocks
    private CourtSinglePointOfEntryService courtSinglePointOfEntryService;

    @Test
    void getCourtsSpoeShouldReturnResults() {
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));
        when(courtSinglePointsOfEntryRepository.findNearestCourtBySpoeAndChildrenAreaOfLaw(51.5, -0.1, "Children"))
            .thenReturn(results);

        List<CourtWithDistance> response = courtSinglePointOfEntryService.getCourtsSpoe(
            51.5,
            -0.1,
            "Children"
        );

        assertThat(response).isEqualTo(results);
        verify(courtSinglePointsOfEntryRepository).findNearestCourtBySpoeAndChildrenAreaOfLaw(51.5, -0.1, "Children");
    }
}
