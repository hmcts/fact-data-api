package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.ImageValidator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageValidatorTest {

    private ImageValidator imageValidator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        imageValidator = new ImageValidator();
    }

    @Test
    void shouldInitialiseWithoutThrowing() {
        assertDoesNotThrow(() -> imageValidator.initialize(null));
    }

    @Test
    void shouldReturnTrueWhenFileIsValidJpeg() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", createImageBytes("jpg"));

        assertTrue(imageValidator.isValid(file, context), "Validator should accept a real JPEG image");
    }

    @Test
    void shouldReturnTrueWhenFileIsValidPng() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", createImageBytes("png"));

        assertTrue(imageValidator.isValid(file, context), "Validator should accept a real PNG image");
    }

    @Test
    void shouldThrowWhenFileIsNull() {
        assertThrows(InvalidFileException.class, () ->
            imageValidator.isValid(null, context)
        );
    }

    @Test
    void shouldThrowWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(InvalidFileException.class, () ->
            imageValidator.isValid(file, context)
        );
    }

    @Test
    void shouldThrowWhenFileContentDoesNotMatchSignature() {
        byte[] bogusContent = "not-an-image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", bogusContent);

        assertThrows(InvalidFileException.class, () ->
            imageValidator.isValid(file, context)
        );
    }

    @Test
    void shouldThrowWhenFileCannotBeRead() throws IOException {
        MultipartFile unreadableFile = mock(MultipartFile.class);
        when(unreadableFile.isEmpty()).thenReturn(false);
        when(unreadableFile.getBytes()).thenThrow(new IOException("Cannot read"));

        assertThrows(InvalidFileException.class, () -> imageValidator.isValid(unreadableFile, context));
    }

    private byte[] createImageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFFFFFF);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, outputStream);
            return outputStream.toByteArray();
        }
    }
}
