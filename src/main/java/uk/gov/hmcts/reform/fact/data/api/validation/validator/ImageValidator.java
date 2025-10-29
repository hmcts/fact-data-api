package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidImage;

import java.io.IOException;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        // no initialisation required at the moment
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        try {
            byte[] b = file.getBytes();
            if (!isPng(b) && !isJpeg(b)) {
                throw new InvalidFileException("Only JPEG or PNG files are allowed");
            }
        } catch (IOException e) {
            throw new InvalidFileException("Unreadable file");
        }

        return true;
    }

    private boolean isPng(byte[] b) {
        return b.length >= 8
            && b[0] == (byte)0x89
            && b[1] == (byte)0x50
            && b[2] == (byte)0x4E
            && b[3] == (byte)0x47
            && b[4] == (byte)0x0D
            && b[5] == (byte)0x0A
            && b[6] == (byte)0x1A
            && b[7] == (byte)0x0A;
    }

    private boolean isJpeg(byte[] b) {
        return b.length >= 4
            && b[0] == (byte)0xFF
            && b[1] == (byte)0xD8
            && b[b.length - 2] == (byte)0xFF
            && b[b.length - 1] == (byte)0xD9;
    }
}

