package uk.gov.hmcts.reform.fact.data.api.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.config.properties.PhotoConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

@ExtendWith(MockitoExtension.class)
class CourtPhotoServiceTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Mock
    private CourtPhotoRepository courtPhotoRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private AzureBlobService azureBlobService;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private AuditUserContext auditUserContext;

    @Mock
    private PhotoConfigurationProperties photoConfigurationProperties;

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
    void setCourtPhotoShouldCreateNewWhenNoneExists() throws IOException {
        UUID courtId = UUID.randomUUID();
        final String uploadedLink = "uploaded-file-link";

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn(uploadedLink);
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream())
            .thenReturn(new ByteArrayInputStream(createImageBytes("jpg", 1, 1)));
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo(uploadedLink);
        assertThat(result.getUpdatedByUserId()).isEqualTo(USER_ID);
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void setCourtPhotoShouldUpdateExistingPhoto() throws IOException {
        UUID courtId = UUID.randomUUID();
        CourtPhoto existing = new CourtPhoto();
        existing.setCourtId(courtId);
        existing.setFileLink("old-link");

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn("new-link");
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream())
            .thenReturn(new ByteArrayInputStream(createImageBytes("jpg", 1, 1)));
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo("new-link");
        assertThat(result.getUpdatedByUserId()).isEqualTo(USER_ID);
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void setCourtPhotoShouldResizeLargeImage() throws IOException {
        UUID courtId = UUID.randomUUID();
        final String uploadedLink = "uploaded-file-link";
        final byte[] imgBytes = createImageBytes("jpg", 512, 512);

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn(uploadedLink);
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(imgBytes));
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(400);

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo(uploadedLink);
        assertThat(result.getUpdatedByUserId()).isEqualTo(USER_ID);
        ArgumentCaptor<MultipartFile> captor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(azureBlobService).uploadFile(eq(courtId.toString()), captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getSize()).isLessThan(imgBytes.length);
        assertThat(captor.getValue().getBytes()).isNotEmpty();
        assertThat(captor.getValue().getInputStream()).isNotNull();
        assertThat(captor.getValue().isEmpty()).isFalse();
        verify(courtPhotoRepository).save(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test.png", "test.jpg", "test.jpeg"})
    void setCourtPhotoShouldDetectFileTypeFromOriginalFilename(String filename) throws IOException {
        UUID courtId = UUID.randomUUID();
        final String uploadedLink = "uploaded-file-link";

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn(uploadedLink);
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream())
            .thenReturn(new ByteArrayInputStream(createImageBytes("png", 1, 1)));
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo(uploadedLink);
        assertThat(result.getUpdatedByUserId()).isEqualTo(USER_ID);
        ArgumentCaptor<MultipartFile> captor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(azureBlobService).uploadFile(eq(courtId.toString()), captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getOriginalFilename()).isEqualTo(filename);
        verify(multipartFile, times(2)).getOriginalFilename();
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void setCourtPhotoShouldResizeLargePngImage() throws IOException {
        UUID courtId = UUID.randomUUID();
        byte[] imgBytes = createImageBytes("png", 512, 512);

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn("new-link");
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(imgBytes));
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getOriginalFilename()).thenReturn("test.png");
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(400);

        courtPhotoService.setCourtPhoto(courtId, multipartFile);

        ArgumentCaptor<MultipartFile> captor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(azureBlobService).uploadFile(eq(courtId.toString()), captor.capture());
        assertThat(captor.getValue().getSize()).isLessThan(imgBytes.length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpg", "image/jpeg"})
    void setCourtPhotoShouldDetectFileTypeFromContentType(String contentType) throws IOException {
        UUID courtId = UUID.randomUUID();
        final String uploadedLink = "uploaded-file-link";

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn(uploadedLink);
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream())
            .thenReturn(new ByteArrayInputStream(createImageBytes("png", 1, 1)));
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getFileLink()).isEqualTo(uploadedLink);
        assertThat(result.getUpdatedByUserId()).isEqualTo(USER_ID);
        ArgumentCaptor<MultipartFile> captor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(azureBlobService).uploadFile(eq(courtId.toString()), captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getContentType()).isEqualTo(contentType);
        verify(multipartFile, times(2)).getContentType();
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void setCourtPhotoShouldFallbackToDefaultJpgWhenFormatIsUnknown() throws IOException {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());
        when(azureBlobService.uploadFile(eq(courtId.toString()), any(MultipartFile.class))).thenReturn("new-link");
        when(auditUserContext.requireUserId()).thenReturn(USER_ID);
        when(courtPhotoRepository.save(any(CourtPhoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(multipartFile.getInputStream())
            .thenReturn(new ByteArrayInputStream(createImageBytes("png", 1, 1)));
        when(multipartFile.getContentType()).thenReturn("application/octet-stream");
        when(multipartFile.getOriginalFilename()).thenReturn("unknown-format.bin");
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        CourtPhoto result = courtPhotoService.setCourtPhoto(courtId, multipartFile);

        assertThat(result.getFileLink()).isEqualTo("new-link");
        verify(azureBlobService).uploadFile(eq(courtId.toString()), any(MultipartFile.class));
        verify(courtPhotoRepository).save(result);
    }

    @Test
    void setCourtPhotoShouldThrowIllegalArgumentWhenImageIOFailsToReadFile() throws IOException {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("not-an-image".getBytes()));
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        try (MockedStatic<ImageIO> imageIoMock = mockStatic(ImageIO.class)) {
            imageIoMock.when(() -> ImageIO.read(any(InputStream.class))).thenReturn(null);

            assertThrows(IllegalArgumentException.class, () ->
                courtPhotoService.setCourtPhoto(courtId, multipartFile)
            );
        }
    }

    @Test
    void setCourtPhotoShouldThrowIllegalArgumentWhenImageIOFailsToWriteFile() throws IOException {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("not-an-image".getBytes()));
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        try (MockedStatic<ImageIO> imageIoMock = mockStatic(ImageIO.class)) {
            imageIoMock.when(() -> ImageIO.write(
                any(RenderedImage.class), anyString(), any(ImageOutputStream.class)
            )).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                courtPhotoService.setCourtPhoto(courtId, multipartFile)
            );
        }
    }

    @Test
    void setCourtPhotoShouldThrowIllegalStateWhenImageWriterIsUnavailable() throws IOException {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("ignored".getBytes()));
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(400);

        try (MockedStatic<ImageIO> imageIoMock = mockStatic(ImageIO.class)) {
            BufferedImage sourceImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
            imageIoMock.when(() -> ImageIO.read(any(InputStream.class))).thenReturn(sourceImage);
            imageIoMock.when(() -> ImageIO.write(
                any(RenderedImage.class), eq("jpg"), any(OutputStream.class)
            )).thenReturn(false);

            assertThrows(IllegalStateException.class, () ->
                courtPhotoService.setCourtPhoto(courtId, multipartFile)
            );
        }
    }

    @Test
    void setCourtPhotoShouldThrowIllegalArgumentWhenIOExceptionOccurs() throws IOException {
        UUID courtId = UUID.randomUUID();

        when(courtService.getCourtById(courtId)).thenReturn(null);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("not-an-image".getBytes()));
        when(photoConfigurationProperties.getMaxWidth()).thenReturn(640);

        try (MockedStatic<ImageIO> imageIoMock = mockStatic(ImageIO.class)) {
            imageIoMock.when(() -> ImageIO.read(any(InputStream.class))).thenThrow(new IOException());

            assertThrows(IllegalArgumentException.class, () ->
                courtPhotoService.setCourtPhoto(courtId, multipartFile)
            );
        }
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

    @Test
    void getPhotoStreamShouldStreamContentAndCloseInputStream() throws IOException {
        UUID courtId = UUID.randomUUID();
        CourtPhoto photo = new CourtPhoto();
        photo.setFileLink("https://example.com/photos/court-photo.png");
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(photo));
        BlobClient blobClient = mock(BlobClient.class);
        BlobInputStream inputStream = mock(BlobInputStream.class);
        byte[] content = {1, 2, 3};
        when(blobContainerClient.getBlobClient("court-photo.png")).thenReturn(blobClient);
        when(blobClient.openInputStream()).thenReturn(inputStream);
        BlobProperties properties = mock(BlobProperties.class);
        when(blobClient.getProperties()).thenReturn(properties);
        when(properties.getContentType()).thenReturn("image/png");
        when(inputStream.transferTo(any(OutputStream.class))).thenAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(content);
            return (long) content.length;
        });

        CourtPhotoService.PhotoStreamDetails details = courtPhotoService.getPhotoStream(courtId);

        assertThat(details.contentType()).isEqualTo("image/png");
        verify(courtService).getCourtById(courtId);
        verify(inputStream, never()).close();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        details.body().writeTo(outputStream);

        assertThat(outputStream.toByteArray()).isEqualTo(content);
        verify(inputStream).close();
    }

    @Test
    void getPhotoStreamShouldCloseInputStreamWhenTransferFails() throws IOException {
        UUID courtId = UUID.randomUUID();
        CourtPhoto photo = new CourtPhoto();
        photo.setFileLink("https://example.com/photos/court-photo.png");
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(photo));
        BlobClient blobClient = mock(BlobClient.class);
        BlobInputStream inputStream = mock(BlobInputStream.class);
        BlobProperties blobProperties = mock(BlobProperties.class);
        IOException failure = new IOException("transfer failed");
        when(blobContainerClient.getBlobClient("court-photo.png")).thenReturn(blobClient);
        when(blobClient.openInputStream()).thenReturn(inputStream);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(inputStream.transferTo(any(OutputStream.class))).thenThrow(failure);

        CourtPhotoService.PhotoStreamDetails details = courtPhotoService.getPhotoStream(courtId);

        assertThat(assertThrows(IOException.class, () -> details.body().writeTo(new ByteArrayOutputStream())))
            .isSameAs(failure);
        verify(inputStream).close();
    }

    @Test
    void getPhotoStreamShouldThrowNotFoundWhenBlobCannotBeOpened() {
        UUID courtId = UUID.randomUUID();
        CourtPhoto photo = new CourtPhoto();
        photo.setFileLink("https://example.com/photos/court-photo.png");
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(photo));
        BlobClient blobClient = mock(BlobClient.class);
        BlobStorageException failure = mock(BlobStorageException.class);
        when(blobContainerClient.getBlobClient("court-photo.png")).thenReturn(blobClient);
        when(blobClient.openInputStream()).thenThrow(failure);

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtPhotoService.getPhotoStream(courtId));

        assertThat(exception.getMessage()).isEqualTo("Photo not found for blob name: court-photo.png");
        assertThat(exception.getCause()).isSameAs(failure);
    }

    @Test
    void getPhotoStreamShouldThrowWhenCourtNotFound() {
        UUID courtId = UUID.randomUUID();
        NotFoundException failure = new NotFoundException("Court not found");
        when(courtService.getCourtById(courtId)).thenThrow(failure);

        assertThat(assertThrows(NotFoundException.class, () -> courtPhotoService.getPhotoStream(courtId)))
            .isSameAs(failure);
        verify(courtPhotoRepository, never()).findCourtPhotoByCourtId(courtId);
    }

    @Test
    void getPhotoStreamShouldThrowWhenPhotoNotFound() {
        UUID courtId = UUID.randomUUID();
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtPhotoService.getPhotoStream(courtId));

        assertThat(exception.getMessage()).isEqualTo("Court photo not found for court ID: " + courtId);
    }

    @Test
    void getPhotoStreamShouldThrowWhenFileLinkIsMissing() {
        UUID courtId = UUID.randomUUID();
        when(courtPhotoRepository.findCourtPhotoByCourtId(courtId)).thenReturn(Optional.of(new CourtPhoto()));

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtPhotoService.getPhotoStream(courtId));

        assertThat(exception.getMessage()).isEqualTo("No photo found for court ID: " + courtId);
    }

    private byte[] createImageBytes(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xFFFFFF);
        image.setRGB(0, 0, width, height, pixels, 0, width);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, outputStream);
            return outputStream.toByteArray();
        }
    }
}
