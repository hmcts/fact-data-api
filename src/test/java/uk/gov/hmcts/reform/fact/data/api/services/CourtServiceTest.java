package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private CourtDetailsRepository courtDetailsRepository;

    @Mock
    private RegionService regionService;

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

        NotFoundException exception = assertThrows(
            NotFoundException.class, () ->
                courtService.getCourtById(courtId)
        );

        assertThat(exception.getMessage()).isEqualTo("Court not found, ID: " + courtId);
    }

    @Test
    void getAllCourtsByIdsReturnsMatchingCourts() {
        UUID courtId1 = UUID.randomUUID();
        UUID courtId2 = UUID.randomUUID();

        Court court1 = new Court();
        court1.setId(courtId1);
        court1.setName("Test Court 1");

        Court court2 = new Court();
        court2.setId(courtId2);
        court2.setName("Test Court 2");

        List<UUID> courtIds = List.of(courtId1, courtId2);
        List<Court> expectedCourts = List.of(court1, court2);

        when(courtRepository.findAllById(courtIds)).thenReturn(expectedCourts);

        List<Court> result = courtService.getAllCourtsByIds(courtIds);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedCourts);
    }

    @Test
    void getAllCourtsByIdsReturnsPartialResultsWhenSomeCourtsNotFound() {
        UUID courtId1 = UUID.randomUUID();
        UUID courtId2 = UUID.randomUUID();

        Court court1 = new Court();
        court1.setId(courtId1);
        court1.setName("Test Court 1");

        List<UUID> courtIds = List.of(courtId1, courtId2);
        List<Court> expectedCourts = List.of(court1);

        when(courtRepository.findAllById(courtIds)).thenReturn(expectedCourts);

        List<Court> result = courtService.getAllCourtsByIds(courtIds);

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(court1);
    }

    @Test
    void getAllCourtsByIdsReturnsEmptyListWhenInputIsEmpty() {
        List<UUID> courtIds = Collections.emptyList();

        when(courtRepository.findAllById(courtIds)).thenReturn(Collections.emptyList());

        List<Court> result = courtService.getAllCourtsByIds(courtIds);

        assertThat(result).isEmpty();
    }

    @Test
    void getFilteredAndPaginatedCourtsShouldReturnOpenCourtsWhenIncludeClosedIsFalse() {
        final Pageable pageable = Pageable.unpaged();
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);
        List<Region> allRegions = List.of(region);
        Court court = new Court();
        court.setName("Example Court");

        when(regionService.getAllRegions()).thenReturn(allRegions);
        when(courtRepository.findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
            anyList(), anyString(), eq(pageable))
        ).thenReturn(new PageImpl<>(List.of(court)));

        Page<Court> result =
            courtService.getFilteredAndPaginatedCourts(pageable, false, null, "Example");

        assertThat(result.getContent()).hasSize(1);
        verify(courtRepository).findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
            anyList(), eq("Example"), eq(pageable)
        );
    }

    @Test
    void getFilteredAndPaginatedCourtsShouldReturnAllCourtsWhenIncludeClosedIsTrue() {
        final Pageable pageable = Pageable.unpaged();
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);
        List<Region> allRegions = List.of(region);
        Court court = new Court();
        court.setName("Example Court");

        when(regionService.getAllRegions()).thenReturn(allRegions);
        when(courtRepository.findByRegionIdInAndNameContainingIgnoreCase(
            anyList(), anyString(), eq(pageable))
        ).thenReturn(new PageImpl<>(List.of(court)));

        Page<Court> result =
            courtService.getFilteredAndPaginatedCourts(pageable, true, null, "Example");

        assertThat(result.getContent()).hasSize(1);
        verify(courtRepository).findByRegionIdInAndNameContainingIgnoreCase(
            anyList(), eq("Example"), eq(pageable)
        );
    }

    @Test
    void getFilteredAndPaginatedCourtsShouldFilterByRegionWhenRegionIdProvided() {
        final Pageable pageable = Pageable.unpaged();
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);
        Court court = new Court();

        when(regionService.getRegionById(regionId)).thenReturn(region);
        when(courtRepository.findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
            anyList(), anyString(), eq(pageable))
        ).thenReturn(new PageImpl<>(List.of(court)));

        Page<Court> result =
            courtService.getFilteredAndPaginatedCourts(pageable, null, regionId.toString(), "Partial");

        assertThat(result.getContent()).hasSize(1);
        verify(regionService).getRegionById(regionId);
    }

    @Test
    void getFilteredAndPaginatedCourtsShouldDefaultNameFilterWhenPartialNameIsNull() {
        Pageable pageable = Pageable.unpaged();
        Region region = new Region();
        region.setId(UUID.randomUUID());

        when(regionService.getAllRegions()).thenReturn(List.of(region));
        when(courtRepository.findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
            anyList(), eq(""), eq(pageable))
        ).thenReturn(Page.empty());

        courtService.getFilteredAndPaginatedCourts(pageable, null, null, null);

        verify(courtRepository).findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
            anyList(), eq(""), eq(pageable)
        );
    }

    @Test
    void getFilteredAndPaginatedCourtsShouldTreatBlankRegionIdAsAllRegions() {
        Pageable pageable = Pageable.unpaged();
        Region region = new Region();
        region.setId(UUID.randomUUID());

        when(regionService.getAllRegions()).thenReturn(List.of(region));
        when(courtRepository.findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
            anyList(), anyString(), eq(pageable))
        ).thenReturn(Page.empty());

        courtService.getFilteredAndPaginatedCourts(pageable, null, "   ", "Name");

        verify(regionService).getAllRegions();
        verify(regionService, never()).getRegionById(any());
    }

    @Test
    void createCourtShouldSetRegionSlugAndOpenFalse() {
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);

        Court input = new Court();
        input.setName("My Test Court");
        input.setRegionId(regionId);

        when(regionService.getRegionById(regionId)).thenReturn(region);
        when(courtRepository.existsBySlug(anyString())).thenReturn(false);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court saved = courtService.createCourt(input);

        assertThat(saved.getOpen()).isFalse();
        assertThat(saved.getRegionId()).isEqualTo(region.getId());
        assertThat(saved.getSlug()).isEqualTo("my-test-court");
        verify(courtRepository).save(saved);
    }

    @Test
    void createCourtShouldGenerateUniqueSlugWhenDuplicateExists() {
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);
        Court input = new Court();
        input.setName("Duplicate Court");
        input.setRegionId(regionId);

        when(regionService.getRegionById(regionId)).thenReturn(region);
        when(courtRepository.existsBySlug("duplicate-court")).thenReturn(true);
        when(courtRepository.existsBySlug("duplicate-court-1")).thenReturn(false);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court result = courtService.createCourt(input);

        assertThat(result.getSlug()).isEqualTo("duplicate-court-1");
    }

    @Test
    void updateCourtShouldUpdateSlugWhenNameChanges() {
        UUID courtId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);

        Court existing = new Court();
        existing.setId(courtId);
        existing.setName("Old Name");
        existing.setSlug("old-name");

        Court updated = new Court();
        updated.setName("New Name");
        updated.setRegionId(regionId);
        updated.setOpen(true);

        when(courtRepository.findById(courtId)).thenReturn(Optional.of(existing));
        when(regionService.getRegionById(regionId)).thenReturn(region);
        when(courtRepository.existsBySlug("new-name")).thenReturn(false);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court result = courtService.updateCourt(courtId, updated);

        assertThat(result.getSlug()).isEqualTo("new-name");
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getOpen()).isTrue();
    }

    @Test
    void updateCourtShouldNotUpdateSlugWhenNameUnchanged() {
        UUID courtId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        Region region = new Region();
        region.setId(regionId);

        Court existing = new Court();
        existing.setId(courtId);
        existing.setName("Same Name");
        existing.setSlug("same-name");

        Court updated = new Court();
        updated.setName("Same Name");
        updated.setRegionId(regionId);
        updated.setOpen(true);

        when(courtRepository.findById(courtId)).thenReturn(Optional.of(existing));
        when(regionService.getRegionById(regionId)).thenReturn(region);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court result = courtService.updateCourt(courtId, updated);

        assertThat(result.getSlug()).isEqualTo("same-name");
        verify(courtRepository, never()).existsBySlug(anyString());
    }

    @Test
    void getCourtsByPrefixAndActiveSearchShouldReturnMatchingCourts() {
        Court court = new Court();
        List<Court> courts = List.of(court);

        when(courtRepository.findCourtByNameStartingWithIgnoreCaseAndOpenOrderByNameAsc("A", true))
            .thenReturn(courts);

        List<Court> response = courtService.getCourtsByPrefixAndActiveSearch("A");

        assertThat(response).isEqualTo(courts);
        verify(courtRepository).findCourtByNameStartingWithIgnoreCaseAndOpenOrderByNameAsc("A", true);
    }

    @Test
    void searchOpenCourtsByNameOrAddressShouldTrimQuery() {
        Court court = new Court();
        List<Court> courts = List.of(court);

        when(courtRepository.searchOpenByNameOrAddress("Test Court")).thenReturn(courts);

        List<Court> response = courtService.searchOpenCourtsByNameOrAddress("  Test Court  ");

        assertThat(response).isEqualTo(courts);
        verify(courtRepository).searchOpenByNameOrAddress("Test Court");
    }

    @Test
    void deleteCourtsByNamePrefixShouldReturnZeroWhenNoMatchesFound() {
        when(courtRepository.findByNameStartingWithIgnoreCase("Missing")).thenReturn(Collections.emptyList());

        long deleted = courtService.deleteCourtsByNamePrefix("Missing");

        assertThat(deleted).isZero();
        verify(courtRepository, never()).deleteAllInBatch(anyList());
    }

    @Test
    void deleteCourtsByNamePrefixShouldTrimInputAndDeleteMatches() {
        Court court = new Court();
        List<Court> courts = List.of(court);
        when(courtRepository.findByNameStartingWithIgnoreCase("Example")).thenReturn(courts);

        long deleted = courtService.deleteCourtsByNamePrefix("  Example ");

        assertThat(deleted).isEqualTo(1);
        verify(courtRepository).findByNameStartingWithIgnoreCase("Example");
        verify(courtRepository).deleteAllInBatch(courts);
    }

    @Test
    void getCourtDetailsByIdReturnsCourtDetailsWhenFound() {
        UUID courtId = UUID.randomUUID();
        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setId(courtId);
        courtDetails.setName("Test Court");

        when(courtDetailsRepository.findById(courtId)).thenReturn(Optional.of(courtDetails));

        CourtDetails result = courtService.getCourtDetailsById(courtId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(courtId);
        assertThat(result.getName()).isEqualTo("Test Court");
    }

    @Test
    void getCourtDetailsByIdThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        UUID courtId = UUID.randomUUID();

        when(courtDetailsRepository.findById(courtId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class, () ->
                courtService.getCourtDetailsById(courtId)
        );

        assertThat(exception.getMessage()).isEqualTo("Court not found, ID: " + courtId);
    }

    @Test
    void getCourtDetailsBySlugReturnsCourtDetailsWhenFound() {
        String courtSlug = "test-court";
        CourtDetails courtDetails = new CourtDetails();
        courtDetails.setSlug(courtSlug);
        courtDetails.setName("Test Court");

        when(courtDetailsRepository.findBySlug(courtSlug)).thenReturn(Optional.of(courtDetails));

        CourtDetails result = courtService.getCourtDetailsBySlug(courtSlug);

        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo(courtSlug);
        assertThat(result.getName()).isEqualTo("Test Court");
    }

    @Test
    void getCourtDetailsBySlugThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        String courtSlug = "missing-court";

        when(courtDetailsRepository.findBySlug(courtSlug)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class, () ->
                courtService.getCourtDetailsBySlug(courtSlug)
        );

        assertThat(exception.getMessage()).isEqualTo("Court not found, slug: " + courtSlug);
    }

    @Test
    void getAllCourtDetailsReturnsCourtDetailsListWhenFound() {
        List<CourtDetails> courtDetailsList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            UUID courtId = UUID.randomUUID();
            CourtDetails courtDetails = new CourtDetails();
            courtDetails.setId(courtId);
            courtDetails.setName(String.format("Test Court %s", (char) (i + 0x41)));
            courtDetailsList.add(courtDetails);
        }

        when(courtDetailsRepository.findAll()).thenReturn(courtDetailsList);

        List<CourtDetails> result = courtService.getAllCourtDetails();

        assertThat(result)
            .isNotNull()
            .isEqualTo(courtDetailsList);
    }
}
