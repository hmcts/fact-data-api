package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CourtAddressService {

    private final CourtAddressRepository courtAddressRepository;
    private final CourtService courtService;
    private final TypesService typesService;
    private final OsService osService;

    public CourtAddressService(CourtAddressRepository courtAddressRepository,
                               CourtService courtService,
                               TypesService typesService,
                               OsService osService) {
        this.courtAddressRepository = courtAddressRepository;
        this.courtService = courtService;
        this.typesService = typesService;
        this.osService = osService;
    }

    /**
     * Find Court Distances through the OS Data provided.
     * Uses the Court Address table to do this, but places the relevant rows into
     * a CourtWithDistance List already for us to use.
     * @param lat the lat
     * @param lng the lng
     * @param limit the limit of rows returned
     * @return A list of CourtWithDistance objects
     */
    public List<CourtWithDistance> findCourtWithDistanceByOsData(double lat, double lng, Integer limit) {
        return courtAddressRepository.findNearestCourts(lat, lng, limit);
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
     * @param courtAddress The address to create.
     * @return Created address.
     * @throws NotFoundException if the court or supplied description type does not exist.
     */
    @Transactional
    public CourtAddress createAddress(UUID courtId, CourtAddress courtAddress) {
        Court court = courtService.getCourtById(courtId);

        courtAddress.setId(null);
        courtAddress.setCourtId(courtId);
        courtAddress.setCourt(court);
        if (courtAddress.getAreasOfLaw() != null) {
            courtAddress.setAreasOfLaw(getValidatedAreasOfLawTypeIds(courtAddress.getAreasOfLaw()));
        }

        if (courtAddress.getCourtTypes() != null) {
            courtAddress.setCourtTypes(getValidatedCourtTypeIds(courtAddress.getCourtTypes()));
        }

        setLatLonFromPostcode(courtAddress);

        return courtAddressRepository.save(courtAddress);
    }

    /**
     * Update an existing address for a court.
     *
     * @param courtId   The court identifier.
     * @param addressId The address identifier.
     * @param courtAddress   Updated address values.
     * @return Updated address.
     * @throws NotFoundException if the court or address does not exist.
     */
    @Transactional
    public CourtAddress updateAddress(UUID courtId, UUID addressId, CourtAddress courtAddress) {
        CourtAddress existing = getAddress(courtId, addressId);
        setNewAddressFieldsOnExistingAddress(existing, courtAddress);

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
        courtAddressRepository.deleteByIdAndCourtId(
            this.getAddress(courtId, addressId).getId(),
            courtService.getCourtById(courtId).getId());
    }

    /**
     * Validates and retrieves a list of areas of law type IDs.
     * Takes a list of UUIDs representing area of law types and validates their existence.
     * Only returns IDs that correspond to valid area of law types in the system.
     *
     * @param areasOfLawIds List of UUIDs to validate
     * @return List of validated area of law type UUIDs
     */
    private List<UUID> getValidatedAreasOfLawTypeIds(List<UUID> areasOfLawIds) {
        return typesService.getAllAreasOfLawTypesByIds(areasOfLawIds)
            .stream()
            .map(AreaOfLawType::getId)
            .toList();
    }

    /**
     * Validates and retrieves a list of court type IDs.
     * Takes a list of UUIDs representing court types and validates their existence.
     * Only returns IDs that correspond to valid court types in the system.
     *
     * @param courtTypeIds List of UUIDs to validate
     * @return List of validated court type UUIDs
     */
    private List<UUID> getValidatedCourtTypeIds(List<UUID> courtTypeIds) {
        return typesService.getAllCourtTypesByIds(courtTypeIds)
            .stream()
            .map(CourtType::getId)
            .toList();
    }

    /**
     * Updates the latitude and longitude coordinates for a court address based on its postcode.
     * Uses the Ordnance Survey API to look up geographic coordinates from the postcode.
     * If valid postcode data is found, updates the address with the corresponding lat/lon values.
     * If postcode is null or no valid coordinates are found, the address remains unchanged.
     *
     * @param address The court address entity to update with geographic coordinates
     */
    private void setLatLonFromPostcode(CourtAddress address) {
        OsData osData = osService.getOsAddressByFullPostcode(address.getPostcode());
        if (osData != null && osData.getResults() != null && !osData.getResults().isEmpty()) {
            OsDpa dpa = osData.getResults().getFirst().getDpa();
            address.setLat(BigDecimal.valueOf(dpa.getLat()));
            address.setLon(BigDecimal.valueOf(dpa.getLng()));
        }
    }

    private void setNewAddressFieldsOnExistingAddress(CourtAddress existing, CourtAddress newAddress) {
        existing.setAddressLine1(newAddress.getAddressLine1());
        existing.setAddressLine2(newAddress.getAddressLine2());
        existing.setTownCity(newAddress.getTownCity());
        existing.setCounty(newAddress.getCounty());
        existing.setPostcode(newAddress.getPostcode());
        existing.setAddressType(newAddress.getAddressType());
        existing.setEpimId(newAddress.getEpimId());

        if (newAddress.getAreasOfLaw() != null) {
            existing.setAreasOfLaw(getValidatedAreasOfLawTypeIds(newAddress.getAreasOfLaw()));
        }

        if (newAddress.getCourtTypes() != null) {
            existing.setCourtTypes(getValidatedCourtTypeIds(newAddress.getCourtTypes()));
        }

        setLatLonFromPostcode(existing);
    }
}
