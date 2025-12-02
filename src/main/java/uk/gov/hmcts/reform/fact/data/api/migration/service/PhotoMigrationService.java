package uk.gov.hmcts.reform.fact.data.api.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyCourtMapping;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtPhotoDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.InMemoryMultipartFile;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.PhotoMigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PhotoMigrationService {

    private final LegacyFactClient legacyFactClient;
    private final LegacyCourtMappingRepository legacyCourtMappingRepository;
    private final CourtPhotoService courtPhotoService;
    private final MigrationAuditRepository migrationAuditRepository;

    private static final String PHOTO_MIGRATION_AUDIT_TYPE = "PHOTO_MIGRATION";

    public PhotoMigrationService(LegacyFactClient legacyFactClient,
                                 LegacyCourtMappingRepository legacyCourtMappingRepository,
                                 CourtPhotoService courtPhotoService,
                                 MigrationAuditRepository migrationAuditRepository) {
        this.legacyFactClient = legacyFactClient;
        this.legacyCourtMappingRepository = legacyCourtMappingRepository;
        this.courtPhotoService = courtPhotoService;
        this.migrationAuditRepository = migrationAuditRepository;
    }

    /**
     * Migrates court photos from the legacy FaCT system to the new system.
     *
     * @return a response summary of the migration results.
     */
    public PhotoMigrationResponse migratePhotos() {
        log.info("Starting photo migration process");

        migrationAuditRepository.findByMigrationName(PHOTO_MIGRATION_AUDIT_TYPE).ifPresent(audit -> {
            log.info("Photo migration process has already been performed on: {}", audit.getUpdatedAt());
            throw new MigrationAlreadyAppliedException("Photo migration has already been performed on: "
                                                           + audit.getUpdatedAt());
        });

        LegacyExportResponse legacyData = legacyFactClient.fetchExport();

        Map<Long, UUID> courtIdMap = legacyCourtMappingRepository.findAll().stream()
            .collect(Collectors.toMap(
                LegacyCourtMapping::getLegacyCourtId,
                LegacyCourtMapping::getCourtId
            ));

        List<PhotoMigrationResponse.Failure> failedMigrations = new ArrayList<>();

        legacyData.getCourts().stream().filter(c -> c.getCourtPhoto() != null).forEach(court -> {
            UUID newCourtId = courtIdMap.get(court.getId());
            CourtPhotoDto courtPhotoDto = court.getCourtPhoto();

            try {
                MultipartFile currentPhoto = getCurrentPhoto(courtPhotoDto.getImagePath());
                courtPhotoService.setCourtPhoto(newCourtId, currentPhoto);
            } catch (Exception ex) {
                failedMigrations.add(
                    new PhotoMigrationResponse.Failure(court.getName(), newCourtId, ex.getMessage())
                );
            }
        });

        log.info("Photo migration process completed. Check API response for details.");
        migrationAuditRepository.save(MigrationAudit
            .builder()
            .migrationName(PHOTO_MIGRATION_AUDIT_TYPE)
            .status(MigrationStatus.SUCCESS)
            .updatedAt(Instant.now())
            .build()
        );
        return new PhotoMigrationResponse("Photo migration completed", failedMigrations);
    }

    /**
     * Fetches the current photo from the given URL and returns it as a MultipartFile.
     *
     * @param photoUrl the URL of the photo to fetch.
     * @return the photo as a MultipartFile.
     */
    private MultipartFile getCurrentPhoto(String photoUrl) {
        try {
            URI uri = URI.create(photoUrl.replace(" ", "%20"));
            byte[] bytes = uri.toURL().openStream().readAllBytes();

            String filename = Paths.get(uri.getPath()).getFileName().toString();
            String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));

            return new InMemoryMultipartFile(
                "file",
                filename,
                contentType != null ? contentType : "application/octet-stream",
                bytes
            );

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to fetch photo from URL: " + photoUrl, e);
        }
    }
}
