package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidImage;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.ImageValidator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageValidatorTest {

    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png"};

    private ImageValidator imageValidator;

    @Mock
    private ValidImage validImageAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        imageValidator = new ImageValidator();
        when(validImageAnnotation.allowedTypes()).thenReturn(ALLOWED_TYPES);
        imageValidator.initialize(validImageAnnotation);
    }

    @Test
    void shouldReturnTrueWhenFileIsValid() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");

        boolean result = imageValidator.isValid(file, context);

        assertTrue(result, "Validator should accept files with allowed content type");
    }

    @Test
    void shouldThrowWhenFileIsNull() {
        assertThrows(InvalidFileException.class, () ->
            imageValidator.isValid(null, context)
        );
    }

    @Test
    void shouldThrowWhenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(InvalidFileException.class, () ->
            imageValidator.isValid(file, context)
        );
    }

    @Test
    void shouldThrowWhenFileHasDisallowedContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThrows(InvalidFileException.class, () ->
            imageValidator.isValid(file, context)
        );
    }
}
