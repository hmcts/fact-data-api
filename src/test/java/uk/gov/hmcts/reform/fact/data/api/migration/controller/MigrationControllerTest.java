package uk.gov.hmcts.reform.fact.data.api.migration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.PhotoMigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.service.MigrationService;
import uk.gov.hmcts.reform.fact.data.api.migration.service.PhotoMigrationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationControllerTest {

    @Mock
    private MigrationService migrationService;

    @Mock
    private PhotoMigrationService photoMigrationService;

    @InjectMocks
    private MigrationController migrationController;

    private PhotoMigrationResponse photoMigrationResponse;

    @BeforeEach
    void setUp() {
        photoMigrationResponse = new PhotoMigrationResponse("done", List.of(
            new PhotoMigrationResponse.Failure("Court A", UUID.randomUUID(), "error")
        ));
    }

    @Test
    void shouldReturnPhotoMigrationResponse() {
        when(photoMigrationService.migratePhotos()).thenReturn(photoMigrationResponse);

        ResponseEntity<PhotoMigrationResponse> response = migrationController.importCourtPhotos();

        assertThat(response.getBody()).isEqualTo(photoMigrationResponse);
        verify(photoMigrationService).migratePhotos();
    }
}
