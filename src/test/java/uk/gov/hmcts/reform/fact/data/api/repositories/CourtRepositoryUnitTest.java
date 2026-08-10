package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourtRepositoryUnitTest {

    @Test
    @DisplayName("findNameAndSlugById returns NameAndSlug when present")
    void findNameAndSlugByIdReturnsNameAndSlugWhenPresent() {
        CourtRepository repo = mock(CourtRepository.class);
        UUID id = UUID.randomUUID();
        CourtRepository.NameAndSlug expected = new CourtRepository.NameAndSlug("Test Court", "test-court");
        when(repo.findNameAndSlugById(id)).thenReturn(Optional.of(expected));

        Optional<CourtRepository.NameAndSlug> result = repo.findNameAndSlugById(id);
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Test Court");
        assertThat(result.get().slug()).isEqualTo("test-court");
    }

    @Test
    @DisplayName("findNameAndSlugById returns empty when not present")
    void findNameAndSlugByIdReturnsEmptyWhenNotPresent() {
        CourtRepository repo = mock(CourtRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findNameAndSlugById(id)).thenReturn(Optional.empty());

        Optional<CourtRepository.NameAndSlug> result = repo.findNameAndSlugById(id);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findFirstByNameIgnoreCase returns court when present")
    void findFirstByNameIgnoreCaseReturnsCourtWhenPresent() {
        CourtRepository repo = mock(CourtRepository.class);
        Court expectedCourt = new Court();
        expectedCourt.setName("Liverpool Civil and Family Court");
        when(repo.findFirstByNameIgnoreCase("liverpool civil and family court"))
            .thenReturn(Optional.of(expectedCourt));

        Optional<Court> result = repo.findFirstByNameIgnoreCase("liverpool civil and family court");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Liverpool Civil and Family Court");
    }

    @Test
    @DisplayName("findFirstByNameIgnoreCase returns empty when not present")
    void findFirstByNameIgnoreCaseReturnsEmptyWhenNotPresent() {
        CourtRepository repo = mock(CourtRepository.class);
        when(repo.findFirstByNameIgnoreCase("missing court")).thenReturn(Optional.empty());

        Optional<Court> result = repo.findFirstByNameIgnoreCase("missing court");
        assertThat(result).isEmpty();
    }
}
