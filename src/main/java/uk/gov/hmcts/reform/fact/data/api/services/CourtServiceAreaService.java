package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourtServiceAreaService {

    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final ServiceAreaService serviceAreaService;
    private final CourtRepository courtRepository;

    /**
     * Finds court service area links by service area id.
     *
     * @param id the service area id
     * @return matching court service areas
     */
    public List<CourtServiceAreas> findByServiceAreaId(UUID id) {
        return courtServiceAreasRepository.findByServiceAreaId(id).stream()
            .map(this::enrichCourtServiceAreas)
            .toList();
    }

    /**
     * Return the court service area details which includes the court_id on the frontend.
     * We can then call a service centre endpoint from the frontend if the
     * catchment type is NATIONAL.
     *
     * @param serviceAreaName The name of the service area.
     * @return A list of courts that link to the service area.
     */
    public List<CourtServiceAreas> findByServiceAreaName(String serviceAreaName) {
        return courtServiceAreasRepository.findByServiceAreaId(
                serviceAreaService.getServiceAreaByName(serviceAreaName).getId()).stream()
            .map(this::enrichCourtServiceAreas)
            .toList();
    }

    // adds the name and slug for the court into the response
    private CourtServiceAreas enrichCourtServiceAreas(CourtServiceAreas courtServiceAreas) {
        courtRepository.findNameAndSlugById(courtServiceAreas.getCourtId())
            .ifPresent(courtInfo -> {
                courtServiceAreas.setCourtName(courtInfo.name());
                courtServiceAreas.setCourtSlug(courtInfo.slug());
            });
        return courtServiceAreas;
    }
}
