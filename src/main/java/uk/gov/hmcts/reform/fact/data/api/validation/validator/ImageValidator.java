package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidImage;

import java.util.Arrays;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private String[] allowedTypes;
    private long maxSize;

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.allowedTypes = constraintAnnotation.allowedTypes();
        this.maxSize = constraintAnnotation.maxSize();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (!Arrays.asList(allowedTypes).contains(file.getContentType().toLowerCase())) {
            throw new InvalidFileException("Only JPEG, JPG, or PNG files are allowed");
        }

        if (file.getSize() > maxSize) {
            throw new InvalidFileException("File size exceeds " + (maxSize / (1024 * 1024)) + " MB");
        }

        return true;
    }
}

