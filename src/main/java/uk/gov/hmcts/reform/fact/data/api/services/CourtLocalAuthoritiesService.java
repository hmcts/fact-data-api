package uk.gov.hmcts.reform.fact.data.api.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

@Service
public class CourtLocalAuthoritiesService {

    private final CourtService courtService;
    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private final AreaOfLawTypeRepository areaOfLawTypeRepository;

    public CourtLocalAuthoritiesService(
        CourtService courtService,
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository,
        LocalAuthorityTypeRepository localAuthorityTypeRepository,
        AreaOfLawTypeRepository areaOfLawTypeRepository) {
        this.courtService = courtService;
        this.courtAreasOfLawRepository = courtAreasOfLawRepository;
        this.courtLocalAuthoritiesRepository = courtLocalAuthoritiesRepository;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
        this.areaOfLawTypeRepository = areaOfLawTypeRepository;
    }

    /**
     * Retrieve local authorities for the allowed areas of law that are enabled for a court.
     *
     * @param courtId The court id to fetch local authorities for.
     * @return A list of court local authority mappings.
     */
    public List<CourtLocalAuthorityDto> getCourtLocalAuthorities(UUID courtId) {
        return buildCourtLocalAuthorityDtoList(
            courtService.getCourtById(courtId).getId(),
            getAllowedAreasOfLawForCourt(courtId)
        );
    }

    /**
     * Update local authorities for a court and its allowed areas of law.
     *
     * @param courtId The court id to update.
     * @param updates The requested updates.
     */
    @Transactional
    public void setCourtLocalAuthorities(UUID courtId, List<CourtLocalAuthorityDto> updates) {
        Court court = courtService.getCourtById(courtId);

        List<AreaOfLawType> allowedAreas = getAllowedAreasOfLawForCourt(courtId);

        Map<UUID, CourtLocalAuthorityDto> incomingByAreaId = updates.stream().collect(
            Collectors.toMap(
                CourtLocalAuthorityDto::getAreaOfLawId,
                dto -> dto
            ));

        List<CourtLocalAuthorities> courtLocalAuthoritiesList = new ArrayList<>();

        for (AreaOfLawType area : allowedAreas) {
            CourtLocalAuthorityDto incomingUpdate =
                Optional.ofNullable(incomingByAreaId.get(area.getId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Missing update for area of law: " + area.getName()
                    ));

            List<UUID> selectedLocalAuthorityIds = incomingUpdate.getLocalAuthorities().stream()
                .filter(LocalAuthoritySelectionDto::getSelected)
                .map(LocalAuthoritySelectionDto::getId)
                .toList();

            courtLocalAuthoritiesList.add(
                CourtLocalAuthorities.builder()
                    .courtId(court.getId())
                    .court(court)
                    .areaOfLawId(area.getId())
                    .localAuthorityIds(selectedLocalAuthorityIds)
                    .build()
            );
        }

        courtLocalAuthoritiesRepository.deleteByCourtId(courtId);
        courtLocalAuthoritiesRepository.saveAll(courtLocalAuthoritiesList);
    }

    /**
     * Get the allowed (based on local authority) and enabled areas of law for a court.
     *
     * @param courtId The court id to get areas of law for.
     * @return The list of allowed areas of law for the court.
     * @throws CourtResourceNotFoundException if no areas of law are set for the court.
     */
    private List<AreaOfLawType> getAllowedAreasOfLawForCourt(UUID courtId) {
        CourtAreasOfLaw courtAreasOfLaw = courtAreasOfLawRepository.findByCourtId(courtId)
            .orElseThrow(() ->
                             new CourtResourceNotFoundException("No areas of law set for the court, ID: " + courtId));

        List<AreaOfLawType> allowedAreas =
            areaOfLawTypeRepository.findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames());

        return areaOfLawTypeRepository.findAllById(courtAreasOfLaw.getAreasOfLaw())
            .stream()
            .filter(allowedAreas::contains)
            .toList();
    }

    /**
     * Build the list of CourtLocalAuthorityDto entries for the allowed areas of law,
     * including the selection state for each local authority.
     *
     * @param courtId The court id to fetch selections for.
     * @param areas The list of allowed and enabled areas of law.
     * @return The list of CourtLocalAuthorityDto objects.
     */
    private List<CourtLocalAuthorityDto> buildCourtLocalAuthorityDtoList(UUID courtId, List<AreaOfLawType> areas) {
        List<LocalAuthorityType> allLocalAuthorities = localAuthorityTypeRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(LocalAuthorityType::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        List<CourtLocalAuthorityDto> localAuthorityDtoList = new ArrayList<>();

        for (AreaOfLawType area : areas) {
            List<UUID> selectedIds = courtLocalAuthoritiesRepository
                .findByCourtIdAndAreaOfLawId(courtId, area.getId())
                .map(CourtLocalAuthorities::getLocalAuthorityIds)
                .orElse(List.of());

            List<LocalAuthoritySelectionDto> selections = allLocalAuthorities.stream()
                .map(la -> LocalAuthoritySelectionDto.from(la, selectedIds))
                .toList();

            localAuthorityDtoList.add(CourtLocalAuthorityDto.from(area, selections));
        }
        return localAuthorityDtoList;
    }
}
