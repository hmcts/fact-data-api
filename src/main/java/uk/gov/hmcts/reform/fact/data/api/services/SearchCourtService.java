package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod.LOCAL_AUTHORITY;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType.REGIONAL;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction.NEAREST;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.CIVIL_POSTCODE_PREFERENCE;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.DEFAULT_AOL_DISTANCE;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.FAMILY_NON_REGIONAL;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.FAMILY_REGIONAL;

@Service
@Slf4j
public class SearchCourtService {

    private final OsService osService;
    private final ServiceAreaService serviceAreaService;
    private final CourtAddressRepository courtAddressRepository;
    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private static final String CHILDCARE_SERVICE_AREA = "Childcare arrangements if you separate from your partner";

    public SearchCourtService(OsService osService,
                              ServiceAreaService serviceAreaService,
                              CourtAddressRepository courtAddressRepository,
                              CourtServiceAreasRepository courtServiceAreasRepository,
                              CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository,
                              LocalAuthorityTypeRepository localAuthorityTypeRepository) {
        this.osService = osService;
        this.serviceAreaService = serviceAreaService;
        this.courtAddressRepository = courtAddressRepository;
        this.courtServiceAreasRepository = courtServiceAreasRepository;
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
    }

    public List<CourtWithDistance> searchPostcodeOnly(String postcode, Integer limit) {
        log.info("searchPostcodeOnly");
        OsDpa osData = osService.getOsAddressByFullPostcode(postcode).getResults().getFirst().getDpa();
        return courtAddressRepository.findNearestCourts(osData.getLat(), osData.getLng(), limit);
    }

    public List<CourtWithDistance> searchWithServiceArea(String postcode, String serviceArea,
                                                         SearchAction action, Integer limit) {
        log.info("searchWithServiceArea");
        log.info("postcode: {}", postcode);
        log.info("serviceArea: {}", serviceArea);
        log.info("action: {}", action);

        OsLocationData osLocationData = osService.getOsLonLatDistrictByPartial(postcode);
        ServiceArea serviceAreaFound = serviceAreaService.getServiceAreaByName(serviceArea);

        log.info("serviceAreaFound: {}", serviceAreaFound);

        if (serviceArea.equalsIgnoreCase(CHILDCARE_SERVICE_AREA)) {
            log.info("Found childcare service");
            return getChildcareCourtsSpoe(osLocationData.getLatitude(), osLocationData.getLongitude());
        }

        SearchStrategy searchStrategy = selectSearchStrategy(
            action,
            osLocationData.getAuthorityName(),
            serviceAreaFound
        );

        log.info(String.valueOf(searchStrategy));

        List<CourtWithDistance> courtWithDistances = executeSearchStrategy(
            osLocationData,
            serviceAreaFound,
            searchStrategy,
            action,
            limit
        );
        log.info(String.valueOf(courtWithDistances));
        return courtWithDistances;
    }

    /**
     * Determine the search strategy to be used.
     *
     * @param action        the action.
     * @param authorityName the authority name derived from OS lookup.
     * @param serviceArea   the service area sent from the frontend.
     * @return a search strategy used to return one or more courts.
     */
    public SearchStrategy selectSearchStrategy(SearchAction action,
                                               String authorityName,
                                               ServiceArea serviceArea) {
        if (action == NEAREST) {
            // Note that DOCUMENTS and UPDATE don't affect business rules here
            // they are only used for sorting logic
            return DEFAULT_AOL_DISTANCE;
        }

        switch (serviceArea.getType()) {
            case CIVIL -> {
                return CIVIL_POSTCODE_PREFERENCE;
            }
            case FAMILY -> {
                if (LOCAL_AUTHORITY.equals(serviceArea.getCatchmentMethod())
                    && !authorityName.isEmpty()) {
                    return courtServiceAreasRepository.findByServiceAreaId(serviceArea.getId())
                        .stream()
                        .anyMatch(courtService -> courtService.getCatchmentType().equals(REGIONAL))
                        ? FAMILY_REGIONAL : FAMILY_NON_REGIONAL;
                }
            }
        }
        return DEFAULT_AOL_DISTANCE;
    }

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
                        List<CourtWithDistance> byAol =
                            courtAddressRepository.findFamilyRegionalByAol(serviceAreaId, lat, lon, aolId);

                        if (!byAol.isEmpty()) {
                            return byAol;
                        }
                        return courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, 1);
                    });
            }
            case FAMILY_NON_REGIONAL: {
                Optional<List<CourtWithDistance>> byLaOpt = getAuthorityID(osLocationData)
                    .map(LocalAuthorityType::getId)
                    .map(laId -> {
                        log.info(
                            "Searching for family non-regional by local authority for {}",
                            osLocationData.getPostcode()
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
            default: {
                log.info(
                    "Default fallback search (if no results found for above search strategy) "
                        + "for {}, {}, {}", searchStrategy, action, osLocationData.getPostcode()
                );
                return courtAddressRepository.findNearestByAreaOfLaw(lat, lon, aolId, limit);
            }
        }
    }

    public List<CourtWithDistance> getChildcareCourtsSpoe(double latitude, double longitude) {
        return courtSinglePointsOfEntryRepository.findNearestCourtBySpoeAndChildrenAreaOfLaw(latitude, longitude);
    }

    private static String stripTrailingCouncil(String name) {
        if (name == null) return null;
        String suffix = " Council";
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }

    private Optional<LocalAuthorityType> getAuthorityID(OsLocationData osLocationData) {
        return localAuthorityTypeRepository
            .findIdByNameIgnoreCase(osLocationData.getAuthorityName()).or(() -> localAuthorityTypeRepository
                .findIdByNameIgnoreCase(stripTrailingCouncil(osLocationData.getAuthorityName())));
    }
}
