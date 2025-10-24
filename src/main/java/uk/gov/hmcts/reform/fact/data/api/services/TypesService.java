package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.util.List;

@Service
@Slf4j
public class TypesService {

    private final AreaOfLawTypeRepository areaOfLawTypeRepository;
    private final CourtTypeRepository courtTypeRepository;
    private final OpeningHoursTypeRepository openingHoursTypeRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final RegionRepository regionRepository;
    private final ServiceAreaRepository serviceAreaRepository;

    public TypesService(
        AreaOfLawTypeRepository areaOfLawTypeRepository,
        CourtTypeRepository courtTypeRepository,
        OpeningHoursTypeRepository openingHoursTypeRepository,
        ContactDescriptionTypeRepository contactDescriptionTypeRepository,
        RegionRepository regionRepository,
        ServiceAreaRepository serviceAreaRepository) {
        this.areaOfLawTypeRepository = areaOfLawTypeRepository;
        this.courtTypeRepository = courtTypeRepository;
        this.openingHoursTypeRepository = openingHoursTypeRepository;
        this.contactDescriptionTypeRepository = contactDescriptionTypeRepository;
        this.regionRepository = regionRepository;
        this.serviceAreaRepository = serviceAreaRepository;
    }

    /**
     * Get all area of law types.
     *
     * @return The area of law types.
     * @throws NotFoundException  if no area of law types exist.
     */
    public List<AreaOfLawType> getAreaOfLawTypes() {
        return areaOfLawTypeRepository.findAll();
    }

    /**
     * Get all court types.
     *
     * @return The court types.
     * @throws NotFoundException if no court types exist.
     */
    public List<CourtType> getCourtTypes() {
        return courtTypeRepository.findAll();
    }

    /**
     * Get all opening hours types.
     *
     * @return The opening hours types.
     * @throws NotFoundException if no opening hours types exist.
     */
    public List<OpeningHourType> getOpeningHoursTypes() {
        return openingHoursTypeRepository.findAll();
    }

    /**
     * Get all contact description types.
     *
     * @return The contact description types.
     * @throws NotFoundException if no contact description types exist.
     */
    public List<ContactDescriptionType> getContactDescriptionTypes() {
        return contactDescriptionTypeRepository.findAll();
    }

    /**
     * Get all regions.
     *
     * @return The regions.
     * @throws NotFoundException if no regions exist.
     */
    public List<Region> getRegions() {
        return regionRepository.findAll();
    }

    /**
     * Get all service areas.
     *
     * @return The service areas.
     * @throws NotFoundException if no service areas exist.
     */
    public List<ServiceArea> getServiceAreas() {
        return serviceAreaRepository.findAll();
    }
}
