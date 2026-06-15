package uk.gov.hmcts.reform.fact.data.api.models;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class StringMultipartFileTest {

    private static final String NAME = "testFile";
    private static final String ORIGINAL_FILE_NAME = "test.csv";
    private static final String CONTENT_TYPE = "text/csv";
    private static final String CONTENT = "This is test content.";

    @Test
    void shouldReturnCorrectName() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        assertThat(file.getName()).isEqualTo(NAME);
    }

    @Test
    void shouldReturnCorrectOriginalFileName() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        assertThat(file.getOriginalFilename()).isEqualTo(ORIGINAL_FILE_NAME);
    }

    @Test
    void shouldReturnCorrectContentType() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        assertThat(file.getContentType()).isEqualTo(CONTENT_TYPE);
    }

    @Test
    void shouldNotBeEmptyWhenContentIsProvided() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        assertThat(file.isEmpty()).isFalse();
    }

    @Test
    void shouldBeEmptyWhenContentIsEmpty() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, "");
        assertThat(file.isEmpty()).isTrue();
    }

    @Test
    void shouldReturnCorrectSize() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        assertThat(file.getSize()).isEqualTo(CONTENT.getBytes().length);
    }

    @Test
    void shouldReturnCorrectBytes() {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        assertThat(file.getBytes()).isEqualTo(CONTENT.getBytes());
    }

    @Test
    void shouldReturnCorrectInputStreamContent() throws IOException {
        StringMultipartFile file = new StringMultipartFile(NAME, ORIGINAL_FILE_NAME, CONTENT_TYPE, CONTENT);
        InputStream inputStream = file.getInputStream();

        byte[] buffer = new byte[CONTENT.length()];
        int bytesRead = inputStream.read(buffer);

        assertThat(bytesRead).isEqualTo(CONTENT.length());
        assertThat(new String(buffer)).isEqualTo(CONTENT);
    }
}

