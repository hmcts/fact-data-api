package uk.gov.hmcts.reform.fact.data.api.models;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * An implementation of the {@link MultipartFile} interface that wraps a {@link String} content.
 *
 * <p>This class is used to represent a file created from a string, which can be then treated as a standard
 * multipart file for operations like uploading to cloud storage.</p>
 */
@Getter
public class StringMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] bytes;

    /**
     * Constructs a new StringMultipartFile.
     *
     * @param name the name of the file
     * @param originalFilename the original filename
     * @param contentType the content type of the file
     * @param content the string content of the file
     */
    public StringMultipartFile(String name, String originalFilename, String contentType, String content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.bytes = content.getBytes();
    }

    @Override
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(java.io.File dest) throws IllegalStateException {
        // This implementation is currently empty as it's not required for current use cases,
        // but the override is necessary for the interface.
    }
}
