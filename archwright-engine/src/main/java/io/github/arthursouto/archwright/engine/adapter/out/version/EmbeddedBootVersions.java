package io.github.arthursouto.archwright.engine.adapter.out.version;

import io.github.arthursouto.archwright.engine.application.port.out.EmbeddedVersions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class EmbeddedBootVersions implements EmbeddedVersions {

    private static final String DEFAULT_RESOURCE = "archwright/catalog/boot-versions.txt";
    private static final String COMMENT_PREFIX = "#";

    private final List<String> versions;

    public EmbeddedBootVersions() {
        this(EmbeddedBootVersions.class.getClassLoader(), DEFAULT_RESOURCE);
    }

    public EmbeddedBootVersions(ClassLoader loader, String resource) {
        this.versions = load(loader, resource);
    }

    @Override
    public List<String> versions() {
        return versions;
    }

    private List<String> load(ClassLoader loader, String resource) {
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("catalog: " + resource + ": not found on classpath");
            }

            return lines(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("catalog: " + resource + ": " + exception.getMessage(), exception);
        }
    }

    private List<String> lines(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith(COMMENT_PREFIX))
                .toList();
    }
}
