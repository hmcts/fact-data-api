package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAreasOfLawServiceTest {

    @Mock
    private CourtAreasOfLawRepository courtAreasOfLawRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private TypesService typesService;

    @InjectMocks
    private CourtAreasOfLawService courtAreasOfLawService;

    private UUID courtId;
    private Court court;
    private CourtAreasOfLaw courtAreasOfLaw;
    private List<AreaOfLawType> areaOfLawTypes;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setCourtId(courtId);
        courtAreasOfLaw.setAreasOfLaw(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));

        areaOfLawTypes = Arrays.asList(
            new AreaOfLawType() {
                {
                    setId(UUID.randomUUID());
                    setName("Test Area Of Law");
                }
            },
            new AreaOfLawType() {
                {
                    setId(UUID.randomUUID());
                    setName("Test Area Of Law");
                }
            }
        );
    }

    @Test
    void getCourtAreasOfLawByCourtIdReturnsAreasOfLawWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(courtId)).thenReturn(Optional.of(courtAreasOfLaw));

        CourtAreasOfLaw result = courtAreasOfLawService.getCourtAreasOfLawByCourtId(courtId);

        assertThat(result).isEqualTo(courtAreasOfLaw);
    }

    @Test
    void getCourtAreasOfLawByCourtIdThrowsNotFoundExceptionWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class, () ->
                courtAreasOfLawService.getCourtAreasOfLawByCourtId(courtId)
        );
    }

    @Test
    void getAreasOfLawStatusByCourtIdReturnsStatusMap() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(courtId)).thenReturn(Optional.of(courtAreasOfLaw));
        when(typesService.getAreaOfLawTypes()).thenReturn(areaOfLawTypes);

        Map<AreaOfLawType, Boolean> result = courtAreasOfLawService.getAreasOfLawStatusByCourtId(courtId);

        assertThat(result).hasSize(2);
        assertThat(result.keySet()).containsExactlyInAnyOrderElementsOf(areaOfLawTypes);
    }

    @Test
    void setCourtAreasOfLawCreatesNewRecord() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtAreasOfLawRepository.save(any(CourtAreasOfLaw.class))).thenReturn(courtAreasOfLaw);

        CourtAreasOfLaw result = courtAreasOfLawService.setCourtAreasOfLaw(courtId, courtAreasOfLaw);

        assertThat(result).isEqualTo(courtAreasOfLaw);
        verify(courtAreasOfLawRepository).save(courtAreasOfLaw);
    }

    @Test
    void setCourtAreasOfLawUpdatesExistingRecord() {
        CourtAreasOfLaw existing = new CourtAreasOfLaw();
        existing.setId(UUID.randomUUID());

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAreasOfLawRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(courtAreasOfLawRepository.save(any(CourtAreasOfLaw.class))).thenReturn(courtAreasOfLaw);

        CourtAreasOfLaw result = courtAreasOfLawService.setCourtAreasOfLaw(courtId, courtAreasOfLaw);

        assertThat(result).isEqualTo(courtAreasOfLaw);
        assertThat(courtAreasOfLaw.getId()).isEqualTo(existing.getId());
    }
}

