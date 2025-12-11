package uk.gov.hmcts.reform.fact.data.api.services;

import static java.util.function.Predicate.not;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidAreaOfLawException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtSinglePointsOfEntryService {

    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private final CourtRepository courtRepository;
    private final AreaOfLawTypeRepository areaOfLawTypeRepository;

    /**
     * Retrieves the configured set of {@link AreaOfLawSelectionDto} for the given court ID.
     *
     * <p>
     * If the configuration doesn't currently exist, it will be initialised.
     *
     * @param courtId           the ID of the Court
     * @return A {@link List} of {@link AreaOfLawSelectionDto}s that reflect the current configuraton.
     */
    public List<AreaOfLawSelectionDto> getCourtSinglePointsOfEntry(UUID courtId) {
        ensureCourt(courtId);

        return courtSinglePointsOfEntryRepository
            .findByCourtId(courtId)
            .map(this::calculateSinglePointsOfEntrySelectionList)
            .orElse(this.initSinglePointsOfEntrySelectionList());
    }

    /**
     * Updates the set of Area of Law IDs associated as Single Points of Entry for the given Court.
     *
     * <p>
     * Uses the selected state {@link List} of {@link AreaOfLawSelectionDto}s to update the current Single Points of
     * Entry configuration for the Court.
     *
     * @param courtId the ID of the Court.
     * @param updates the {@link List} of {@link AreaOfLawSelectionDto} updates.
     */
    @Transactional
    public void updateCourtSinglePointsOfEntry(UUID courtId, List<AreaOfLawSelectionDto> updates) {
        ensureCourt(courtId);

        // Only assign those areas of law that are selected
        List<AreaOfLawSelectionDto> selected = updates.stream().filter(AreaOfLawSelectionDto::getSelected).toList();

        // validate that selected updates are for allowed areas of law
        validateUpdateData(selected);

        // retrieve or init the spoe data for the court
        CourtSinglePointsOfEntry courtSinglePointsOfEntry = courtSinglePointsOfEntryRepository.findByCourtId(courtId)
            .orElse(CourtSinglePointsOfEntry.builder().courtId(courtId).build());

        courtSinglePointsOfEntry.setAreasOfLaw(selected.stream().map(AreaOfLawSelectionDto::getId).toList());
        courtSinglePointsOfEntryRepository.save(courtSinglePointsOfEntry);
    }

    private void validateUpdateData(final List<AreaOfLawSelectionDto> selected) {

        // duplicate check
        if (selected.stream().map(AreaOfLawSelectionDto::getId).distinct().count() != selected.size()) {
            throw new DuplicatedListItemException("Duplicated Area of Law in selection");
        }

        // check that updates only contain those
        List<UUID> allowedAreaIDs =
            areaOfLawTypeRepository.findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames()).stream()
                .map(AreaOfLawType::getId)
                .toList();

        List<AreaOfLawSelectionDto> invalidAreaIDs = selected.stream()
            .filter(a -> !allowedAreaIDs.contains(a.getId()))
            .toList();

        if (!invalidAreaIDs.isEmpty()) {
            throw new InvalidAreaOfLawException(
                "Invalid Area(s) of Law specified in Single Points of Entry configuration"
                    + invalidAreaIDs.stream()
                    .map(AreaOfLawSelectionDto::getName)
                    .collect(Collectors.joining(", "))
            );
        }
    }

    private void ensureCourt(final UUID courtId) {
        if (!courtRepository.existsById(courtId)) {
            throw new NotFoundException("Court not found, ID: " + courtId);
        }
    }

    private List<AreaOfLawSelectionDto> calculateSinglePointsOfEntrySelectionList(
        CourtSinglePointsOfEntry singlePointsOfEntry) {

        List<AreaOfLawType> selectedAreas = Optional.ofNullable(singlePointsOfEntry.getAreasOfLaw())
            .map(areaOfLawTypeRepository::findAllById)
            .orElse(Collections.emptyList());

        List<AreaOfLawType> allowedAreas =
            areaOfLawTypeRepository.findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames());

        List<AreaOfLawType> unselectedAreas = allowedAreas.stream().filter(not(selectedAreas::contains))
            .toList();

        return Stream.of(
            selectedAreas.stream().map(AreaOfLawSelectionDto::asSelected).toList(),
            unselectedAreas.stream().map(AreaOfLawSelectionDto::asUnselected).toList()
        ).flatMap(Collection::stream).toList();
    }

    private List<AreaOfLawSelectionDto> initSinglePointsOfEntrySelectionList() {
        return areaOfLawTypeRepository
            .findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames())
            .stream()
            .map(AreaOfLawSelectionDto::asUnselected)
            .toList();
    }
}
