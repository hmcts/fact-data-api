package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.config.properties.PhotoConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourtPhotoService {

    private final CourtPhotoRepository courtPhotoRepository;
    private final CourtService courtService;
    private final AzureBlobService azureBlobService;
    private final AuditUserContext auditUserContext;
    private final PhotoConfigurationProperties photoConfigurationProperties;

    /**
     * Get a court photo by court id.
     *
     * @param courtId The id of the court.
     * @return The court photo entity.
     * @throws NotFoundException if no court found with the given id or no photo found for the court.
     */
    public CourtPhoto getCourtPhotoByCourtId(UUID courtId) {
        courtService.getCourtById(courtId);
        return courtPhotoRepository.findCourtPhotoByCourtId(courtId).orElseThrow(
            () -> new NotFoundException("Court photo not found for court ID: " + courtId)
        );
    }

    /**
     * Set or update a court photo.
     *
     * @param courtId The id of the court.
     * @param file The court photo to save.
     * @return The saved court photo entity.
     */
    public CourtPhoto setCourtPhoto(UUID courtId, MultipartFile file) {
        courtService.getCourtById(courtId);

        MultipartFile resizedFile = resizeIfNeeded(file);

        CourtPhoto courtPhoto = courtPhotoRepository.findCourtPhotoByCourtId(courtId)
            .orElse(new CourtPhoto());

        courtPhoto.setCourtId(courtId);
        courtPhoto.setFileLink(azureBlobService.uploadFile(courtId.toString(), resizedFile));
        courtPhoto.setUpdatedByUserId(auditUserContext.requireUserId());

        return courtPhotoRepository.save(courtPhoto);
    }

    /**
     * Delete a court photo by court id.
     *
     * @param courtId The id of the court.
     * @throws NotFoundException if no court found with the given id or no photo found for the court.
     */
    public void deleteCourtPhotoByCourtId(UUID courtId) {
        courtService.getCourtById(courtId);

        log.info("Deleting court photo for court ID: {}", courtId);
        CourtPhoto courtPhoto = getCourtPhotoByCourtId(courtId);

        azureBlobService.deleteBlob(courtPhoto.getCourtId().toString());
        courtPhotoRepository.deleteById(courtPhoto.getId());
    }

    private MultipartFile resizeIfNeeded(MultipartFile file) {
        String format = detectImageFormat(file.getOriginalFilename(), file.getContentType());
        int maxWidth = photoConfigurationProperties.getMaxWidth();

        try {
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (sourceImage == null) {
                throw new IllegalArgumentException("Unable to read image content");
            }

            if (sourceImage.getWidth() <= maxWidth) {
                return file;
            }

            int newHeight = (int) Math.round((double) sourceImage.getHeight() * maxWidth / sourceImage.getWidth());

            int imageType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage resizedImage = new BufferedImage(maxWidth, newHeight, imageType);

            Graphics2D graphics = resizedImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, maxWidth, newHeight, null);
            graphics.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(resizedImage, format, outputStream)) {
                throw new IllegalStateException("No ImageIO writer found for format: " + format);
            }

            return new InMemoryMultipartFile(
                file.getName(),
                file.getOriginalFilename(),
                file.getContentType(),
                outputStream.toByteArray()
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to process uploaded image", ex);
        }
    }

    private String detectImageFormat(String originalFilename, String contentType) {
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("png")) {
                return "png";
            }
            if (lower.contains("jpeg") || lower.contains("jpg")) {
                return "jpg";
            }
        }

        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) {
                return "png";
            }
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return "jpg";
            }
        }

        // Defensive fallback; upstream validation should already ensure png/jpg.
        return "jpg";
    }

    /**
     * In-Memory wrapper around multipart file content to allow resizing and
     * uploading without writing to disk.
     */
    @Getter
    @AllArgsConstructor
    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
