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
import java.util.Objects;
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
     * @param serviceArea    The service area, for example money claims.
     * @param searchStrategy The Search Strategy.
     * @param action         The action, for example documents.
     * @param limit          the amount of rows to return for relevant queries.
     * @return A list of CourtWithDistance objects.
     */
    public List<CourtWithDistance> executeSearchStrategy(OsLocationData osLocationData, ServiceArea serviceArea,
                                                         SearchStrategy searchStrategy, SearchAction action,
                                                         int limit) {
        final double lat = osLocationData.getLatitude();
        final double lon = osLocationData.getLongitude();
        final UUID aolId = serviceArea.getAreaOfLawId();
        return switch (searchStrategy) {
            case DEFAULT_AOL_DISTANCE -> courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
            case CIVIL_POSTCODE_PREFERENCE -> executeCivilSearchStrategy(
                osLocationData.getPostcode(),
                lat,
                lon,
                serviceArea.getId(),
                limit,
                aolId
            );
            case FAMILY_REGIONAL -> executeFamilyRegionalSearchStrategy(
                serviceArea,
                osLocationData,
                lat,
                lon,
                limit,
                aolId
            );
            case FAMILY_NON_REGIONAL -> {
                List<CourtWithDistance> results = executeFamilyNonRegionalSearchStrategy(
                    osLocationData,
                    serviceArea,
                    lat,
                    lon,
                    limit,
                    aolId
                );
                if (results.isEmpty()) {
                    log.debug(
                        "Default fallback search (if no results found for determined search strategy) "
                            + "for {}, {}, {}",
                        searchStrategy,
                        action,
                        osLocationData.getPostcode()
                    );
                }
                yield results.isEmpty()
                    ? courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit)
                    : results;
            }
        };
    }

    /**
     * Where we have a civil search strategy, execute it according to the following rules:
     * The court area of law needs to be CIVIL.
     * Court addresses need to have postcodes that match (obv), else it defaults to top 10 search.
     * A postcode ladder is used so it starts from the full postcode and works its way down.
     *
     * @param postcode the postcode
     * @param lat the latitude
     * @param lon the longitude
     * @param serviceAreaId the service area id
     * @param limit the limit/amount of rows to be returned
     * @param aolId the area of law id
     * @return returns a list of court with distance objects
     */
    private List<CourtWithDistance> executeCivilSearchStrategy(String postcode, double lat, double lon,
                                                               UUID serviceAreaId, int limit, UUID aolId) {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode(postcode);
        log.debug("Postcode ladder provided for CIVIl search: {}", ladder);
        List<CourtWithDistance> results = courtAddressRepository.findCivilByPartialPostcodeBestTier(
            serviceAreaId,
            lat,
            lon,
            ladder.getMinusUnitNoSpace(),
            ladder.getOutCodeNoSpace(),
            ladder.getAreacodeNoSpace(),
            limit
        );

        return !results.isEmpty()
            ? results
            : courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
    }

    /**
     * Execute the family regional search strategy according to the following rules:
     * One of the court service areas need to match a regional service area by ID.
     * The area of law needs to be FAMILY.
     * The catchment also needs to be local-authority.
     *
     * @param serviceArea The service area
     * @param osLocationData The ordnance survey location data
     * @param lat the latitude
     * @param lon the longitude
     * @param limit the limit
     * @param aolId the area of law id
     * @return a list of court with distance objects
     */
    private List<CourtWithDistance> executeFamilyRegionalSearchStrategy(ServiceArea serviceArea,
                                                                        OsLocationData osLocationData, double lat,
                                                                        double lon, int limit, UUID aolId) {
        UUID serviceAreaId = serviceArea.getId();
        return getAuthorityID(osLocationData)
            .map(LocalAuthorityType::getId)
            .map(localAuthorityId -> courtAddressRepository.findFamilyRegionalByLocalAuthority(
                serviceAreaId,
                lat,
                lon,
                aolId,
                localAuthorityId
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

    /**
     * Execute the non-regional search strategy.
     * This will perform a search that requires the local authority to be present.
     *
     * @param osLocationData the ordnance survey location data.
     * @param serviceArea the service area.
     * @param lat the latitude.
     * @param lon the longitude.
     * @param limit the amount of rows to return.
     * @param aolId the area of law id.
     * @return a list of court with distance objects.
     */
    private List<CourtWithDistance> executeFamilyNonRegionalSearchStrategy(OsLocationData osLocationData,
                                                                           ServiceArea serviceArea, double lat,
                                                                           double lon, int limit, UUID aolId) {
        Optional<List<CourtWithDistance>> byLaOpt = getAuthorityID(osLocationData)
            .map(LocalAuthorityType::getId)
            .map(localAuthorityId -> {
                log.debug(
                    "Searching for family non-regional by local authority ({}) for {}",
                    serviceArea.getAreaOfLaw().getName(),
                    osLocationData.getPostcode()
                );
                return courtAddressRepository.findFamilyNonRegionalByLocalAuthority(
                    lat,
                    lon,
                    aolId,
                    localAuthorityId,
                    limit
                );
            })
            .filter(results -> !results.isEmpty());

        if (byLaOpt.isPresent()) {
            return byLaOpt.get();
        }
        log.debug("Searching for family regional returned no results: {}", serviceArea);
        return List.of();
    }

    /**
     * Return the Authority ID for the provided OS Location Data.
     *
     * @param osLocationData the OsLocationData object that contains the authority.
     * @return A LocalAuthorityType if found.
     */
    private Optional<LocalAuthorityType> getAuthorityID(OsLocationData osLocationData) {
        return localAuthorityTypeRepository
            .findIdByNameIgnoreCase(osLocationData.getAuthorityName())
            .or(() -> localAuthorityTypeRepository.findIdByNameIgnoreCase(
                stripTrailingCouncil(osLocationData.getAuthorityName())
            ));
    }

    /**
     * Utility method to Strip a trailing council. This is for edge cases which could
     * feasibly happen, but likely won't. Still better to be safe than sorry!
     *
     * @param name The name of the authority
     * @return The stripped name
     */
    private static String stripTrailingCouncil(String name) {
        String safeName = Objects.requireNonNullElse(name, "");
        String suffix = " Council";
        return safeName.endsWith(suffix)
                ? safeName.substring(0, safeName.length() - suffix.length())
                : safeName;
    }
}
