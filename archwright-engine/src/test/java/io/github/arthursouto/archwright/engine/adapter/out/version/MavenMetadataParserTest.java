package io.github.arthursouto.archwright.engine.adapter.out.version;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenMetadataParserTest {

    private final MavenMetadataParser parser = new MavenMetadataParser();

    @Test
    void readsEveryVersionOfTheSampleFileInOrder() throws IOException {
        assertThat(parser.versions(sample())).containsExactly(
                "1.0.0.RELEASE", "2.6.15", "2.7.17", "2.7.18", "3.5.15", "3.5.16",
                "4.1.0", "4.1.1", "4.1.2", "4.2.0-M1", "4.2.0-RC2");
    }

    @Test
    void ignoresLatestAndReleaseElements() throws IOException {
        assertThat(parser.versions(sample())).doesNotContain("latest", "release");
    }

    @Test
    void trimsVersionsAndSkipsEmptyOnes() throws IOException {
        String xml = "<metadata><versioning><versions><version> 3.5.3 </version><version/><version>  </version></versions></versioning></metadata>";

        assertThat(parser.versions(xml)).containsExactly("3.5.3");
    }

    @Test
    void failsWhenThereIsNoVersionsElement() {
        assertThatThrownBy(() -> parser.versions("<metadata><versioning/></metadata>"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("versions");
    }

    @Test
    void failsOnMalformedXml() {
        assertThatThrownBy(() -> parser.versions("<metadata><versions>"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("invalid maven-metadata.xml");
    }

    @Test
    void refusesDocumentTypeDeclarationsToAvoidEntityAttacks() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE metadata [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <metadata><versioning><versions><version>&secret;</version></versions></versioning></metadata>
                """;

        assertThatThrownBy(() -> parser.versions(xml)).isInstanceOf(IOException.class);
    }

    private String sample() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("maven-metadata-sample.xml")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
