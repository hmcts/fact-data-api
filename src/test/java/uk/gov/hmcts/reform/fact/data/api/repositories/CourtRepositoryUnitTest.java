package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
