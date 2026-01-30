package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAddressServiceTest {

    @Mock
    private CourtAddressRepository courtAddressRepository;

    @InjectMocks
    private CourtAddressService courtAddressService;

    @Test
    void findCourtWithDistanceByOsDataShouldReturnResults() {
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));
        when(courtAddressRepository.findNearestCourts(51.5, -0.1, 10)).thenReturn(results);

        List<CourtWithDistance> response = courtAddressService.findCourtWithDistanceByOsData(51.5, -0.1, 10);

        assertThat(response).isEqualTo(results);
        verify(courtAddressRepository).findNearestCourts(51.5, -0.1, 10);
    }
}
