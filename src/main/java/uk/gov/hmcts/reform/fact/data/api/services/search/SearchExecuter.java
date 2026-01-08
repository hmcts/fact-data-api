package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class SearchExecuter {

    private final CourtAddressRepository courtAddressRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;

    public SearchExecuter(CourtAddressRepository courtAddressRepository,
                          LocalAuthorityTypeRepository localAuthorityTypeRepository) {
        this.courtAddressRepository = courtAddressRepository;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
    }

    /**
     * Based on the SearchStrategy determined, perform the relevant business logic.
     *
     * @param osLocationData The data returned from OS based on a postcode search.
     * @param serviceArea The service area, for example money claims.
     * @param searchStrategy The Search Strategy.
     * @param action The action, for example documents.
     * @param limit the amount of rows to return for relevant queries.
     * @return A list of CourtWithDistance objects.
     */
    public List<CourtWithDistance> executeSearchStrategy(OsLocationData osLocationData, ServiceArea serviceArea,
                                                         SearchStrategy searchStrategy, SearchAction action,
                                                         int limit) {
        final double lat = osLocationData.getLatitude();
        final double lon = osLocationData.getLongitude();
        final UUID aolId = serviceArea.getAreaOfLawId();

        switch (searchStrategy) {
            case DEFAULT_AOL_DISTANCE: {
                return courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
            }
            case CIVIL_POSTCODE_PREFERENCE: {
                PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode(osLocationData.getPostcode());
                log.debug("Postcode ladder provided for CIVIl search: {}", ladder);
                List<CourtWithDistance> results = courtAddressRepository.findCivilByPartialPostcodeBestTier(
                    serviceArea.getId(),
                    lat, lon,
                    ladder.minusUnitNoSpace(),
                    ladder.outcodeNoSpace(),
                    ladder.areacodeNoSpace(),
                    limit
                );

                return !results.isEmpty()
                    ? results
                    : courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
            }
            case FAMILY_REGIONAL: {
                UUID serviceAreaId = serviceArea.getId();
                return getAuthorityID(osLocationData)
                    .map(LocalAuthorityType::getId)
                    .map(laId -> courtAddressRepository.findFamilyRegionalByLocalAuthority(
                        serviceAreaId, lat, lon, aolId, laId
                    ))
                    .filter(list -> !list.isEmpty())
                    .orElseGet(() -> {
                        log.debug("Finding family regional for {}", serviceArea);
                        List<CourtWithDistance> byAol =
                            courtAddressRepository.findFamilyRegionalByAol(serviceAreaId, lat, lon, aolId);
                        if (!byAol.isEmpty()) {
                            return byAol;
                        }
                        log.debug("Finding family regional fallback for {}", serviceArea);
                        return courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
                    });
            }
            case FAMILY_NON_REGIONAL: {
                Optional<List<CourtWithDistance>> byLaOpt = getAuthorityID(osLocationData)
                    .map(LocalAuthorityType::getId)
                    .map(laId -> {
                        log.debug(
                            "Searching for family non-regional by local authority ({}) for {}",
                            serviceArea.getAreaOfLaw().getName(), osLocationData.getPostcode()
                        );
                        return courtAddressRepository.findFamilyNonRegionalByLocalAuthority(
                            lat, lon, aolId, laId, limit
                        );
                    })
                    .filter(results -> !results.isEmpty());

                if (byLaOpt.isPresent()) {
                    return byLaOpt.get();
                }
            }
            // fall through
            default: {
                log.debug(
                    "Default fallback search (if no results found for determined search strategy) "
                        + "for {}, {}, {}", searchStrategy, action, osLocationData.getPostcode()
                );
                return courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
            }
        }
    }

    /**
     * Return the Authority ID for the provided OS Location Data.
     *
     * @param osLocationData the OsLocationData object that contains the authority.
     * @return A LocalAuthorityType if found.
     */
    private Optional<LocalAuthorityType> getAuthorityID(OsLocationData osLocationData) {
        return localAuthorityTypeRepository
            .findIdByNameIgnoreCase(osLocationData.getAuthorityName()).or(() -> localAuthorityTypeRepository
                .findIdByNameIgnoreCase(stripTrailingCouncil(osLocationData.getAuthorityName())));
    }

    /**
     * Utility method to Strip a trailing council. This is for edge cases which could
     * feasibly happen, but likely won't. Still better to be safe than sorry!
     *
     * @param name The name of the authority
     * @return The stripped name
     */
    private static String stripTrailingCouncil(String name) {
        if (name == null) {
            return null;
        }
        String suffix = " Council";
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }
}
