package uk.gov.hmcts.reform.fact.data.api.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMultipartFileTest {

    @Test
    void reportsEmptyAndNonEmptyContentCorrectly() {
        InMemoryMultipartFile emptyFile = new InMemoryMultipartFile("name", "empty.txt", "text/plain", new byte[0]);
        InMemoryMultipartFile nonEmptyFile = new InMemoryMultipartFile(
            "name",
            "non-empty.txt",
            "text/plain",
            "content".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(emptyFile.isEmpty()).isTrue();
        assertThat(nonEmptyFile.isEmpty()).isFalse();
        assertThat(nonEmptyFile.getSize()).isEqualTo(7);
    }

    @Test
    void returnsDefensiveCopyFromGetBytes() {
        byte[] originalContent = "abc".getBytes(StandardCharsets.UTF_8);
        InMemoryMultipartFile file = new InMemoryMultipartFile("name", "file.txt", "text/plain", originalContent);

        byte[] returned = file.getBytes();
        returned[0] = 'z';

        assertThat(file.getBytes()).containsExactly('a', 'b', 'c');
    }

    @Test
    void returnsInputStreamForStoredContent() throws IOException {
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        InMemoryMultipartFile file = new InMemoryMultipartFile("name", "file.txt", "text/plain", content);

        assertThat(file.getInputStream().readAllBytes()).containsExactly(content);
    }

    @Test
    void transferToWritesFileContent(@TempDir Path tempDir) throws IOException {
        byte[] content = "blob-content".getBytes(StandardCharsets.UTF_8);
        InMemoryMultipartFile file = new InMemoryMultipartFile("name", "file.txt", "text/plain", content);
        Path destination = tempDir.resolve("uploaded.bin");

        file.transferTo(destination.toFile());

        assertThat(Files.readAllBytes(destination)).containsExactly(content);
    }
}

