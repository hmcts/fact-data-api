package uk.gov.hmcts.reform.fact.data.api.migration.service;

import feign.FeignException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResult;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSummary;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

@Service
public class MigrationService {

    private static final String DATA_MIGRATION_NAME = "legacy-data-migration";

    private final LegacyFactClient legacyFactClient;
    private final MigrationAuditRepository migrationAuditRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReferenceDataImporter referenceDataImporter;
    private final CourtMigrationHelper courtMigrationHelper;

    public MigrationService(
        LegacyFactClient legacyFactClient,
        RegionRepository regionRepository,
        AreaOfLawTypeRepository areaOfLawTypeRepository,
        ServiceAreaRepository serviceAreaRepository,
        LegacyServiceRepository legacyServiceRepository,
        LegacyCourtMappingRepository legacyCourtMappingRepository,
        LocalAuthorityTypeRepository localAuthorityTypeRepository,
        ContactDescriptionTypeRepository contactDescriptionTypeRepository,
        OpeningHourTypeRepository openingHourTypeRepository,
        CourtServiceAreasRepository courtServiceAreasRepository,
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository,
        CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository,
        CourtProfessionalInformationRepository courtProfessionalInformationRepository,
        CourtCodesRepository courtCodesRepository,
        CourtDxCodeRepository courtDxCodeRepository,
        CourtFaxRepository courtFaxRepository,
        MigrationAuditRepository migrationAuditRepository,
        TransactionTemplate transactionTemplate,
        CourtService courtService
    ) {
        this.legacyFactClient = legacyFactClient;
        this.migrationAuditRepository = migrationAuditRepository;
        this.transactionTemplate = transactionTemplate;
        this.referenceDataImporter = new ReferenceDataImporter(
            regionRepository,
            areaOfLawTypeRepository,
            serviceAreaRepository,
            legacyServiceRepository,
            localAuthorityTypeRepository,
            contactDescriptionTypeRepository,
            openingHourTypeRepository
        );
        this.courtMigrationHelper = new CourtMigrationHelper(
            regionRepository,
            courtServiceAreasRepository,
            courtAreasOfLawRepository,
            courtSinglePointsOfEntryRepository,
            courtLocalAuthoritiesRepository,
            courtProfessionalInformationRepository,
            courtCodesRepository,
            courtDxCodeRepository,
            courtFaxRepository,
            legacyCourtMappingRepository,
            courtService
        );
    }

    public MigrationSummary migrate() {
        guardAgainstDuplicateExecution();

        markMigrationStatus(MigrationStatus.IN_PROGRESS);
        try {
            final LegacyExportResponse exportResponse = Optional.ofNullable(fetchLegacyExport())
                .orElseThrow(() -> new MigrationClientException("Legacy export response was empty"));

            MigrationSummary summary = transactionTemplate.execute(status -> persistExport(exportResponse));
            markMigrationStatus(MigrationStatus.SUCCESS);
            return summary;
        } catch (RuntimeException ex) {
            markMigrationStatus(MigrationStatus.FAILED);
            throw ex;
        }
    }

    MigrationSummary persistExport(LegacyExportResponse exportResponse) {
        final MigrationContext context = new MigrationContext();
        referenceDataImporter.importReferenceData(exportResponse, context);
        int courtsMigrated = courtMigrationHelper.migrateCourts(exportResponse.getCourts(), context);

        MigrationResult result = new MigrationResult(
            courtsMigrated,
            context.getCourtAreasOfLawMigrated(),
            context.getCourtServiceAreasMigrated(),
            context.getCourtLocalAuthoritiesMigrated(),
            context.getCourtSinglePointsOfEntryMigrated(),
            context.getCourtProfessionalInformationMigrated(),
            context.getCourtCodesMigrated(),
            context.getCourtDxCodesMigrated(),
            context.getCourtFaxMigrated()
        );

        return new MigrationSummary(result);
    }

    private void guardAgainstDuplicateExecution() {
        Optional<MigrationAudit> audit = migrationAuditRepository.findByMigrationName(DATA_MIGRATION_NAME);
        if (audit.isEmpty()) {
            return;
        }
        MigrationStatus status = audit.get().getStatus();
        if (status == MigrationStatus.SUCCESS) {
            throw new MigrationAlreadyAppliedException(
                "Legacy data migration has already been applied successfully. "
                    + "If you need to rerun it, reset the migration audit record first."
            );
        }
        if (status == MigrationStatus.IN_PROGRESS) {
            throw new MigrationAlreadyAppliedException(
                "Legacy data migration is already running. Please wait for it to finish."
            );
        }
    }

    private void markMigrationStatus(MigrationStatus status) {
        MigrationAudit audit = migrationAuditRepository.findByMigrationName(DATA_MIGRATION_NAME)
            .orElseGet(() -> MigrationAudit.builder()
                .migrationName(DATA_MIGRATION_NAME)
                .build());
        audit.setStatus(status);
        audit.setUpdatedAt(Instant.now());
        migrationAuditRepository.save(audit);
    }

    private LegacyExportResponse fetchLegacyExport() {
        try {
            return legacyFactClient.fetchExport();
        } catch (FeignException ex) {
            throw new MigrationClientException("Failed to fetch data from legacy FaCT", ex);
        }
    }
}
