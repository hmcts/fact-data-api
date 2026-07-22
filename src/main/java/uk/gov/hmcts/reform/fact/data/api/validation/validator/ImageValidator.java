package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

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

        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {

            if (imageInputStream == null) {
                throw new InvalidFileException("Unreadable file");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new InvalidFileException("Only JPEG or PNG files are allowed");
            }

            ImageReader reader = readers.next();
            String format;
            try {
                reader.setInput(imageInputStream, true, true);
                format = reader.getFormatName();

                if (!isAllowedFormat(format)) {
                    throw new InvalidFileException("Only JPEG or PNG files are allowed");
                }

                // Force actual decode metadata/content read - truncated streams fail here.
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new InvalidFileException("Unreadable file");
                }
            } finally {
                reader.dispose();
            }

            return true;
        } catch (InvalidFileException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileException("Unreadable file");
        }
    }

    private boolean isAllowedFormat(String format) {
        if (format == null) {
            return false;
        }
        String normalized = format.toLowerCase(Locale.ROOT);
        return "jpeg".equals(normalized) || "jpg".equals(normalized) || "png".equals(normalized);
    }
}
