package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourtDetailsViewService {

    private final TypesService typesService;

    public CourtDetailsViewService(TypesService typesService) {
        this.typesService = typesService;
    }

    /**
     * Prepares the court details response for the slug endpoint.
     *
     * <p>JsonView annotations cannot expand UUID arrays (areas of law, court types), so those IDs need to be
     * looked up and expanded into objects. For the small number of relations that do exist, we avoid global
     * eager loading so other endpoints do not change shape or pick up extra joins. This keeps the expanded
     * payload limited to the details endpoint only.
     *
     * @param courtDetails the court details entity to prepare
     * @return the same entity after enrichment, or null if nothing was provided
     */
    public CourtDetails prepareDetailsView(CourtDetails courtDetails) {
        if (courtDetails == null) {
            return null;
        }

        enrichOpeningHourTypes(courtDetails.getCourtOpeningHours());
        enrichContactDescriptions(courtDetails.getCourtContactDetails());
        enrichAddressTypes(courtDetails.getCourtAddresses());
        enrichCourtAreasOfLaw(courtDetails.getCourtAreasOfLaw());
        return courtDetails;
    }

    /**
     * Pull in opening hour types by ID so the details endpoint can return names without
     * forcing a global eager fetch.
     *
     * @param openingHours the opening hours records for a court
     */
    private void enrichOpeningHourTypes(List<CourtOpeningHours> openingHours) {
        if (openingHours == null || openingHours.isEmpty()) {
            return;
        }

        List<UUID> openingHourTypeIds = openingHours.stream()
            .map(CourtOpeningHours::getOpeningHourTypeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<UUID, OpeningHourType> openingHourTypesById = openingHourTypeIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getOpeningHourTypesByIds(openingHourTypeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(OpeningHourType::getId, Function.identity()));

        openingHours.forEach(openingHour -> {
            OpeningHourType openingHourType = openingHourTypesById.get(openingHour.getOpeningHourTypeId());
            if (openingHourType == null && openingHour.getOpeningHourTypeId() != null) {
                OpeningHourType fallback = new OpeningHourType();
                fallback.setId(openingHour.getOpeningHourTypeId());
                openingHourType = fallback;
            }
            openingHour.setOpeningHourTypeDetails(openingHourType);
        });
    }

    /**
     * Pull in contact description types by ID so the details endpoint can return names without
     * altering other endpoints.
     *
     * @param contactDetails the contact details records for a court
     */
    private void enrichContactDescriptions(List<CourtContactDetails> contactDetails) {
        if (contactDetails == null || contactDetails.isEmpty()) {
            return;
        }

        List<UUID> descriptionTypeIds = contactDetails.stream()
            .map(CourtContactDetails::getCourtContactDescriptionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<UUID, ContactDescriptionType> descriptionTypesById = descriptionTypeIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getContactDescriptionTypesByIds(descriptionTypeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ContactDescriptionType::getId, Function.identity()));

        contactDetails.forEach(contactDetail -> {
            UUID descriptionTypeId = contactDetail.getCourtContactDescriptionId();
            ContactDescriptionType descriptionType = descriptionTypesById.get(descriptionTypeId);
            if (descriptionType == null && descriptionTypeId != null) {
                ContactDescriptionType fallback = new ContactDescriptionType();
                fallback.setId(descriptionTypeId);
                descriptionType = fallback;
            }
            contactDetail.setCourtContactDescriptionDetails(descriptionType);
        });
    }

    /**
     * Address records store UUID arrays for areas of law and court types. This expands those IDs into
     * objects for the details response, while preserving order and list length.
     *
     * @param addresses the addresses associated with a court
     */
    private void enrichAddressTypes(List<CourtAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }

        List<UUID> areaOfLawIds = addresses.stream()
            .flatMap(address -> safeList(address.getAreasOfLaw()).stream())
            .distinct()
            .toList();

        List<UUID> courtTypeIds = addresses.stream()
            .flatMap(address -> safeList(address.getCourtTypes()).stream())
            .distinct()
            .toList();

        Map<UUID, AreaOfLawType> areasOfLawById = areaOfLawIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getAllAreasOfLawTypesByIds(areaOfLawIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AreaOfLawType::getId, Function.identity()));

        Map<UUID, CourtType> courtTypesById = courtTypeIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getAllCourtTypesByIds(courtTypeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CourtType::getId, Function.identity()));

        addresses.forEach(address -> {
            address.setAreasOfLawDetails(
                safeList(address.getAreasOfLaw()).stream()
                    .map(id -> areasOfLawById.getOrDefault(id, createAreaOfLawStub(id)))
                    .toList()
            );

            address.setCourtTypeDetails(
                safeList(address.getCourtTypes()).stream()
                    .map(id -> courtTypesById.getOrDefault(id, createCourtTypeStub(id)))
                    .toList()
            );
        });
    }

    /**
     * Court-level areas of law are stored as UUID arrays; this expands them into objects for the details
     * response while keeping the list order stable.
     *
     * @param courtAreasOfLaw the court-level areas of law rows to expand
     */
    private void enrichCourtAreasOfLaw(List<CourtAreasOfLaw> courtAreasOfLaw) {
        if (courtAreasOfLaw == null || courtAreasOfLaw.isEmpty()) {
            return;
        }

        List<UUID> areaOfLawIds = courtAreasOfLaw.stream()
            .flatMap(area -> safeList(area.getAreasOfLaw()).stream())
            .distinct()
            .toList();

        Map<UUID, AreaOfLawType> areasOfLawById = areaOfLawIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getAllAreasOfLawTypesByIds(areaOfLawIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AreaOfLawType::getId, Function.identity()));

        courtAreasOfLaw.forEach(area -> area.setAreasOfLawDetails(
            safeList(area.getAreasOfLaw()).stream()
                .map(id -> areasOfLawById.getOrDefault(id, createAreaOfLawStub(id)))
                .toList()
        ));
    }

    /**
     * Create a minimal area of law object so consumers still see the UUID when a lookup is missing.
     *
     * @param id the area of law UUID
     * @return a stub area of law object or null if empty
     */
    private AreaOfLawType createAreaOfLawStub(UUID id) {
        if (id == null) {
            return null;
        }
        AreaOfLawType fallback = new AreaOfLawType();
        fallback.setId(id);
        return fallback;
    }

    /**
     * Create a minimal court type object so consumers still see the UUID when a lookup is missing.
     *
     * @param id the court type UUID
     * @return a stub court type object or null if empty
     */
    private CourtType createCourtTypeStub(UUID id) {
        if (id == null) {
            return null;
        }
        CourtType fallback = new CourtType();
        fallback.setId(id);
        return fallback;
    }

    /**
     * Treat a null list as empty so call sites can stream safely without extra checks.
     * Basically because we do not like nulls.
     *
     * @param input the list to normalize
     * @param <T> the list element type
     * @return an empty list if input is {@code null}, otherwise the same list
     */
    private <T> List<T> safeList(List<T> input) {
        return input == null ? Collections.emptyList() : input;
    }
}
