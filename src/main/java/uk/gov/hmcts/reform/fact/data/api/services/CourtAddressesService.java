package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CourtAddressesService {

    private final CourtAddressRepository courtAddressRepository;
    private final CourtService courtService;
    private final TypesService typesService;

    public CourtAddressesService(CourtAddressRepository courtAddressRepository,
                                 CourtService courtService,
                                 TypesService typesService) {
        this.courtAddressRepository = courtAddressRepository;
        this.courtService = courtService;
        this.typesService = typesService;
    }

    /**
     * Retrieve all addresses for a specific court.
     *
     * @param courtId The court identifier.
     * @return List of address records for the court.
     * @throws NotFoundException if the court does not exist.
     */
    public List<CourtAddress> getAddresses(UUID courtId) {
        courtService.getCourtById(courtId);
        return courtAddressRepository.findByCourtId(courtId);
    }

    /**
     * Retrieve a single address entry by court and address identifiers.
     *
     * @param courtId   The court identifier.
     * @param addressId The address identifier.
     * @return Matching address detail.
     * @throws NotFoundException if the court or address does not exist.
     */
    public CourtAddress getAddress(UUID courtId, UUID addressId) {
        courtService.getCourtById(courtId);
        return courtAddressRepository.findByIdAndCourtId(addressId, courtId).orElseThrow(
            () -> new NotFoundException(
                "Court address not found, addressId: " + addressId + ", courtId: " + courtId
            )
        );
    }

    /**
     * Persist a new address for a court.
     *
     * @param courtId The court identifier.
     * @param request The address to create.
     * @return Created address.
     * @throws NotFoundException if the court or supplied description type does not exist.
     */
    @Transactional
    public CourtAddress createAddress(UUID courtId, CourtAddress request) {
        Court court = courtService.getCourtById(courtId);

        request.setId(null);
        request.setCourtId(courtId);
        request.setCourt(court);
        request.setAreasOfLaw(getValidatedAreasOfLawTypeIds(request.getAreasOfLaw()));
        request.setCourtTypes(getValidatedCourtTypeIds(request.getCourtTypes()));

        log.info("Creating address for court {}", courtId);
        return courtAddressRepository.save(request);
    }

    /**
     * Update an existing address for a court.
     *
     * @param courtId   The court identifier.
     * @param addressId The address identifier.
     * @param request   Updated address values.
     * @return Updated address.
     * @throws NotFoundException if the court or address does not exist.
     */
    @Transactional
    public CourtAddress updateAddress(UUID courtId, UUID addressId, CourtAddress request) {
        CourtAddress existing = getAddress(courtId, addressId);

        existing.setAddressLine1(request.getAddressLine1());
        existing.setAddressLine2(request.getAddressLine2());
        existing.setTownCity(request.getTownCity());
        existing.setCounty(request.getCounty());
        existing.setPostcode(request.getPostcode());
        existing.setEpimId(request.getEpimId());
        existing.setLat(request.getLat());
        existing.setLon(request.getLon());
        existing.setAddressType(request.getAddressType());
        existing.setAreasOfLaw(getValidatedAreasOfLawTypeIds(request.getAreasOfLaw()));
        existing.setCourtTypes(getValidatedCourtTypeIds(request.getCourtTypes()));

        log.info("Updating address {} for court {}", addressId, courtId);
        return courtAddressRepository.save(existing);
    }

    /**
     * Remove an address for a court.
     *
     * @param courtId   The court identifier.
     * @param addressId The address identifier.
     * @throws NotFoundException if the court or address does not exist.
     */
    @Transactional
    public void deleteAddress(UUID courtId, UUID addressId) {
        courtService.getCourtById(courtId);
        if (!courtAddressRepository.existsByIdAndCourtId(addressId, courtId)) {
            throw new NotFoundException(
                "Court address not found, addressId: " + addressId + ", courtId: " + courtId
            );
        }

        log.info("Deleting address {} for court {}", addressId, courtId);
        courtAddressRepository.deleteByIdAndCourtId(addressId, courtId);
    }

    private List<UUID> getValidatedAreasOfLawTypeIds(List<UUID> areasOfLawIds) {
        if (areasOfLawIds == null) {
            return null;
        }

        return typesService.getAllAreasOfLawTypesByIds(areasOfLawIds)
            .stream()
            .map(AreaOfLawType::getId)
            .toList();
    }

    private List<UUID> getValidatedCourtTypeIds(List<UUID> courtTypeIds) {
        if (courtTypeIds == null) {
            return null;
        }

        return typesService.getAllCourtTypesByIds(courtTypeIds)
            .stream()
            .map(CourtType::getId)
            .toList();
    }
}
