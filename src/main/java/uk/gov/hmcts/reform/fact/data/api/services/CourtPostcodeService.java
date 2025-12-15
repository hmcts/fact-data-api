package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeMigrationRequestException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeListDto;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeMoveDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPostcodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtPostcodeService {

    private final CourtPostcodeRepository courtPostcodeRepository;
    private final CourtRepository courtRepository;

    public List<CourtPostcode> getPostcodesByCourtId(final UUID courtId) {
        ensureCourts(courtId);
        return courtPostcodeRepository.getCourtPostcodeByCourtId(courtId);
    }

    @Transactional
    public void addPostcodesToCourt(PostcodeListDto courtPostcodes, final UUID courtId) {

        ensureCourts(courtId);
        validatePostcodeList(courtPostcodes);

        List<CourtPostcode> additionalPostcodes = createNormalisedPostcodesToAddList(courtPostcodes, courtId);

        // save the new postcodes
        if (!additionalPostcodes.isEmpty()) {
            courtPostcodeRepository.saveAll(additionalPostcodes);
        }

        // No error when there are no postcodes to add. The request is technically complete
    }

    @Transactional
    public void removePostcodesFromCourt(PostcodeListDto courtPostcodes, final UUID courtId) {

        ensureCourts(courtId);
        validatePostcodeList(courtPostcodes);

        // An empty delete request is not considered a fail state
        if (!courtPostcodes.getPostcodes().isEmpty()) {
            courtPostcodeRepository.deleteAll(createNormalisedPostcodesToDeleteList(courtPostcodes, courtId));
        }
    }

    @Transactional
    public void migratePostcodes(PostcodeMoveDto migrationData) {
        validatePostcodeMigrationData(migrationData);

        List<CourtPostcode> postcodesToDelete = createNormalisedPostcodesToDeleteList(
            migrationData.getPostcodeList(),
            migrationData.getSourceCourtId()
        );

        // build the list of postcodes to add. Unlike with the delete code,
        // we aren't going to raise an error if some or all of the postcodes
        // already exist on the destination court.
        List<CourtPostcode> additionalPostcodes = createNormalisedPostcodesToAddList(
            migrationData.getPostcodeList(),
            migrationData.getDestinationCourtId()
        );

        // make the changes
        courtPostcodeRepository.deleteAll(postcodesToDelete);
        if (!additionalPostcodes.isEmpty()) {
            courtPostcodeRepository.saveAll(additionalPostcodes);
        }
    }

    // -- Util --

    // returns the set of postcodes that need to be added, excluding from the
    // list those postcodes that already exist on the court
    private List<CourtPostcode> createNormalisedPostcodesToAddList(PostcodeListDto courtPostcodes, UUID courtId) {

        // get the existing set of assigned postcodes
        List<String> existingPostcodes = courtPostcodeRepository.findAllByCourtId(courtId).stream()
            .map(CourtPostcode::getPostcode)
            .toList();

        // filter out the existing postcodes from the list to add, then
        // convert them to the entity representation, assigned to the court
        return courtPostcodes.getPostcodes().stream()
            .map(this::normalisePostcode)
            .filter(postcode -> !existingPostcodes.contains(postcode))
            .map(postcode -> CourtPostcode.builder().courtId(courtId).postcode(postcode).build())
            .toList();
    }

    // return the set of postcodes that need to be removed, excluding from the
    // list those postcodes that don't currently exist on the court
    private List<CourtPostcode> createNormalisedPostcodesToDeleteList(PostcodeListDto courtPostcodes, UUID courtId) {

        // normalise the delete request so that we can more easily compare, and make
        // it mutable in case we need to modify it for the error response
        List<String> normalisedPostcodes = courtPostcodes.getPostcodes().stream().map(this::normalisePostcode).collect(
            Collectors.toCollection(ArrayList::new));

        // get the existing set of assigned postcodes filtered by those we wish to remove
        List<CourtPostcode> postcodesToDelete = courtPostcodeRepository.findAllByCourtIdAndPostcodeIn(
            courtId,
            normalisedPostcodes
        );

        if (postcodesToDelete.size() != normalisedPostcodes.size()) {
            // we've been asked to delete postcodes that aren't assigned
            normalisedPostcodes.removeAll(postcodesToDelete.stream()
                                              .map(CourtPostcode::getPostcode)
                                              .toList());
            throw new NotFoundException("Unassigned postcode(s) in delete request ("
                                            + String.join(", ", normalisedPostcodes)
                                            + ") for court: " + courtId.toString());
        }

        return postcodesToDelete;
    }

    private String normalisePostcode(String postcode) {
        return postcode.replaceAll("(\\w+)\\s*(\\d[a-zA-Z]{2})", "$1 $2").toUpperCase();
    }

    // -- Validation --

    private void validatePostcodeMigrationData(PostcodeMoveDto migrationData) {
        if (migrationData.getSourceCourtId().equals(migrationData.getDestinationCourtId())) {
            throw new InvalidPostcodeMigrationRequestException("Source and Destination court IDs are the same");
        }
        ensureCourts(migrationData.getSourceCourtId(), migrationData.getDestinationCourtId());
        validatePostcodeList(migrationData.getPostcodeList());
    }

    private void validatePostcodeList(final PostcodeListDto courtPostcodes) {
        // duplicate check
        if (courtPostcodes.getPostcodes().stream().map(String::toUpperCase).distinct().count()
            != courtPostcodes.getPostcodes().size()) {
            throw new DuplicatedListItemException("Duplicated Postcode in payload list");
        }
    }

    private void ensureCourts(UUID... courts) {
        for (UUID courtId : courts) {
            if (!courtRepository.existsById(courtId)) {
                throw new NotFoundException("Court not found, ID: " + courtId);
            }
        }
    }

}
