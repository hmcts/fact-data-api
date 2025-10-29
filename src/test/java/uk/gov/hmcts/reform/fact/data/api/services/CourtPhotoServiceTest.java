package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtPhotoServiceTest {

    @Mock
    private CourtPhotoRepository courtPhotoRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private AzureBlobService azureBlobService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CourtPhotoService courtPhotoService;

    @Test
    void getCourtPhotoByCourtIdShouldReturnPhotoWhenFound() {
        UUID courtId = UUID.randomUUID();
        CourtPhoto courtPhoto = new CourtPhoto();
        courtPhoto.setCourtId(courtId);
        courtPhoto.setFileLink("photo-url");

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(courtPhoto));

        CourtPhoto result = courtPhotoService.getCourtPhotoByCourtId(courtId);

        assertThat(result).isNotNull();
        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo("photo-url");
    }

    @Test
    void getCourtPhotoByCourtIdShouldThrowWhenPhotoNotFound() {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtPhotoService.getCourtPhotoByCourtId(courtId)
        );

        assertThat(exception.getMessage()).isEqualTo("Court photo not found for court ID: " + courtId);
    }

    @Test
    void setCourtPhotoShouldCreateNewWhenNoneExists() {
        UUID courtId = UUID.randomUUID();
        String uploadedLink = "uploaded-file-link";

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), eq(multipartFile))).thenReturn(uploadedLink);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo(uploadedLink);
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void setCourtPhotoShouldUpdateExistingPhoto() {
        UUID courtId = UUID.randomUUID();
        CourtPhoto existing = new CourtPhoto();
        existing.setCourtId(courtId);
        existing.setFileLink("old-link");

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(azureBlobService.uploadFile(eq(courtId.toString()), eq(multipartFile))).thenReturn("new-link");
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo("new-link");
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void deleteCourtPhotoByCourtIdShouldDeleteSuccessfully() {
        UUID courtId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        CourtPhoto courtPhoto = new CourtPhoto();
        courtPhoto.setId(photoId);
        courtPhoto.setCourtId(courtId);
        courtPhoto.setFileLink("file-link");

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(courtPhoto));

        courtPhotoService.deleteCourtPhotoByCourtId(courtId);

        verify(azureBlobService).deleteBlob(courtId.toString());
        verify(courtPhotoRepository).deleteById(photoId);
    }

    @Test
    void deleteCourtPhotoByCourtIdShouldThrowWhenPhotoNotFound() {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtPhotoService.deleteCourtPhotoByCourtId(courtId)
        );

        assertThat(exception.getMessage()).isEqualTo("Court photo not found for court ID: " + courtId);
    }
}
