package uk.gov.hmcts.reform.fact.data.api.migration.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMultipartFileTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExposeFileMetadataAndContent() throws IOException {
        byte[] content = "sample-content".getBytes(StandardCharsets.UTF_8);
        InMemoryMultipartFile multipartFile = new InMemoryMultipartFile(
            "photo",
            "court.jpg",
            "image/jpeg",
            content
        );

        assertThat(multipartFile.getName()).isEqualTo("photo");
        assertThat(multipartFile.getOriginalFilename()).isEqualTo("court.jpg");
        assertThat(multipartFile.getContentType()).isEqualTo("image/jpeg");
        assertThat(multipartFile.isEmpty()).isFalse();
        assertThat(multipartFile.getSize()).isEqualTo(content.length);
        assertThat(multipartFile.getBytes()).isEqualTo(content);
        try (InputStream inputStream = multipartFile.getInputStream()) {
            assertThat(inputStream.readAllBytes()).isEqualTo(content);
        }

        Path destination = tempDir.resolve("copied.jpg");
        multipartFile.transferTo(destination.toFile());
        assertThat(Files.readAllBytes(destination)).isEqualTo(content);
    }

    @Test
    void shouldReportEmptyWhenNoContent() {
        InMemoryMultipartFile emptyFile = new InMemoryMultipartFile(
            "photo",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        assertThat(emptyFile.isEmpty()).isTrue();
        assertThat(emptyFile.getSize()).isZero();
    }
}
