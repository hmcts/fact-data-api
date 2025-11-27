package uk.gov.hmcts.reform.fact.data.api.migration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyCourtMapping;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtPhotoDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.PhotoMigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoMigrationServiceTest {

    private static final String MIGRATION_NAME = "PHOTO_MIGRATION";

    @Mock
    private LegacyFactClient legacyFactClient;
    @Mock
    private LegacyCourtMappingRepository legacyCourtMappingRepository;
    @Mock
    private CourtPhotoService courtPhotoService;
    @Mock
    private MigrationAuditRepository migrationAuditRepository;
    @InjectMocks
    private PhotoMigrationService photoMigrationService;

    @TempDir
    Path tempDir;

    @Test
    void shouldThrowWhenMigrationAlreadyApplied() {
        MigrationAudit audit = MigrationAudit.builder()
            .migrationName(MIGRATION_NAME)
            .status(MigrationStatus.SUCCESS)
            .updatedAt(Instant.now())
            .build();
        when(migrationAuditRepository.findByMigrationName(MIGRATION_NAME)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> photoMigrationService.migratePhotos())
            .isInstanceOf(MigrationAlreadyAppliedException.class);

        verify(legacyFactClient, never()).fetchExport();
        verify(legacyCourtMappingRepository, never()).findAll();
    }

    @Test
    void shouldMigrateAllPhotosAndRecordAuditSuccess() throws IOException {
        when(migrationAuditRepository.findByMigrationName(MIGRATION_NAME)).thenReturn(Optional.empty());
        List<LegacyCourtMapping> mappings = List.of(
            buildMapping(1L, UUID.randomUUID()),
            buildMapping(2L, UUID.randomUUID())
        );
        when(legacyCourtMappingRepository.findAll()).thenReturn(mappings);
        List<CourtDto> courts = List.of(
            buildCourt(1L, "Court one", createPhotoFile("first")),
            buildCourt(2L, "Court two", createPhotoFile("second"))
        );
        when(legacyFactClient.fetchExport()).thenReturn(buildLegacyExportResponse(courts));

        PhotoMigrationResponse response = photoMigrationService.migratePhotos();

        assertThat(response.message()).isEqualTo("Photo migration completed");
        assertThat(response.failedFiles()).isEmpty();
        mappings.forEach(mapping ->
            verify(courtPhotoService).setCourtPhoto(eq(mapping.getCourtId()), any(MultipartFile.class))
        );
        ArgumentCaptor<MigrationAudit> auditCaptor = ArgumentCaptor.forClass(MigrationAudit.class);
        verify(migrationAuditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getStatus()).isEqualTo(MigrationStatus.SUCCESS);
    }

    @Test
    void shouldCaptureFailuresWithoutStoppingRemainingMigrations() throws IOException {
        UUID courtId = UUID.randomUUID();
        when(migrationAuditRepository.findByMigrationName(MIGRATION_NAME)).thenReturn(Optional.empty());
        when(legacyCourtMappingRepository.findAll()).thenReturn(List.of(buildMapping(10L, courtId)));
        List<CourtDto> courts = List.of(buildCourt(10L, "Problem court", createPhotoFile("content")));
        when(legacyFactClient.fetchExport()).thenReturn(buildLegacyExportResponse(courts));
        doThrow(new IllegalStateException("storage unavailable"))
            .when(courtPhotoService).setCourtPhoto(eq(courtId), any(MultipartFile.class));

        PhotoMigrationResponse response = photoMigrationService.migratePhotos();

        assertThat(response.failedFiles()).hasSize(1);
        PhotoMigrationResponse.Failure failure = response.failedFiles().get(0);
        assertThat(failure.name()).isEqualTo("Problem court");
        assertThat(failure.id()).isEqualTo(courtId);
        assertThat(failure.error()).contains("storage unavailable");
    }

    private LegacyCourtMapping buildMapping(Long legacyId, UUID courtId) {
        return LegacyCourtMapping.builder()
            .legacyCourtId(legacyId)
            .courtId(courtId)
            .build();
    }

    private CourtDto buildCourt(Long legacyId, String name, String photoPath) {
        return new CourtDto(
            legacyId,
            name,
            null,
            Boolean.TRUE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new CourtPhotoDto(photoPath),
            null
        );
    }

    private LegacyExportResponse buildLegacyExportResponse(List<CourtDto> courts) {
        return new LegacyExportResponse(courts, null, null, null, null, null, null, null, null);
    }

    private String createPhotoFile(String content) throws IOException {
        Path file = Files.createTempFile(tempDir, "photo", ".jpg");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toUri().toString();
    }
}
