package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAddressRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCentreAddressService {

    private final ServiceCentreAddressRepository serviceCentreAddressRepository;
    private final ServiceCentreService serviceCentreService;
    private final OsService osService;

    /**
     * Retrieve all addresses for a specific service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @return List of address records for the service centre.
     * @throws NotFoundException if the service centre does not exist.
     */
    public List<ServiceCentreAddress> getAddresses(UUID serviceCentreId) {
        serviceCentreService.getServiceCentreById(serviceCentreId);
        return serviceCentreAddressRepository.findByServiceCentreId(serviceCentreId);
    }

    /**
     * Retrieve a single address entry by service centre and address identifiers.
     *
     * @param serviceCentreId The service centre identifier.
     * @param addressId The address identifier.
     * @return Matching address detail.
     * @throws NotFoundException if the service centre or address does not exist.
     */
    public ServiceCentreAddress getAddress(UUID serviceCentreId, UUID addressId) {
        serviceCentreService.getServiceCentreById(serviceCentreId);
        return serviceCentreAddressRepository.findByIdAndServiceCentreId(addressId, serviceCentreId).orElseThrow(
            () -> new NotFoundException(
                "Service centre address not found, addressId: " + addressId + ", serviceCentreId: " + serviceCentreId
            )
        );
    }

    /**
     * Persist a new address for a service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @param serviceCentreAddress The address to create.
     * @return Created address.
     * @throws NotFoundException if the service centre does not exist.
     */
    @Transactional
    public ServiceCentreAddress createAddress(UUID serviceCentreId, ServiceCentreAddress serviceCentreAddress) {
        ServiceCentre serviceCentre = serviceCentreService.getServiceCentreById(serviceCentreId);

        serviceCentreAddress.setId(null);
        serviceCentreAddress.setServiceCentreId(serviceCentreId);
        serviceCentreAddress.setServiceCentre(serviceCentre);
        setLatLonFromPostcode(serviceCentreAddress);

        return serviceCentreAddressRepository.save(serviceCentreAddress);
    }

    /**
     * Update an existing address for a service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @param addressId The address identifier.
     * @param serviceCentreAddress Updated address values.
     * @return Updated address.
     * @throws NotFoundException if the service centre or address does not exist.
     */
    @Transactional
    public ServiceCentreAddress updateAddress(UUID serviceCentreId,
                                              UUID addressId,
                                              ServiceCentreAddress serviceCentreAddress) {
        ServiceCentreAddress existing = getAddress(serviceCentreId, addressId);
        setNewAddressFieldsOnExistingAddress(existing, serviceCentreAddress);

        return serviceCentreAddressRepository.save(existing);
    }

    /**
     * Remove an address for a service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @param addressId The address identifier.
     * @throws NotFoundException if the service centre or address does not exist.
     */
    @Transactional
    public void deleteAddress(UUID serviceCentreId, UUID addressId) {
        serviceCentreAddressRepository.deleteByIdAndServiceCentreId(
            getAddress(serviceCentreId, addressId).getId(),
            serviceCentreId
        );
    }

    private void setLatLonFromPostcode(ServiceCentreAddress address) {
        OsData osData = osService.getOsAddressByFullPostcode(address.getPostcode());
        if (osData != null && osData.getResults() != null && !osData.getResults().isEmpty()) {
            OsDpa dpa = osData.getResults().getFirst().getDpa();
            address.setLat(BigDecimal.valueOf(dpa.getLat()));
            address.setLon(BigDecimal.valueOf(dpa.getLng()));
        }
    }

    private void setNewAddressFieldsOnExistingAddress(ServiceCentreAddress existing, ServiceCentreAddress newAddress) {
        existing.setAddressLine1(newAddress.getAddressLine1());
        existing.setAddressLine2(newAddress.getAddressLine2());
        existing.setTownCity(newAddress.getTownCity());
        existing.setCounty(newAddress.getCounty());
        existing.setPostcode(newAddress.getPostcode());
        existing.setAddressType(newAddress.getAddressType());

        setLatLonFromPostcode(existing);
    }
}
