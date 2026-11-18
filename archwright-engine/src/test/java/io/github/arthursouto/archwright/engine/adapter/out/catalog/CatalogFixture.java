package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class CatalogFixture {

    static final String STYLES = "styles.yaml";
    static final String PROFILES = "boot-profiles.yaml";
    static final String DEPENDENCIES = "dependencies.yaml";
    static final String EXTRAS = "extras.yaml";

    private static final String BASE_PATH = "archwright/catalog";

    private final Map<String, String> files = new LinkedHashMap<>();

    private CatalogFixture() {
    }

    static CatalogFixture valid() {
        return new CatalogFixture()
                .with(STYLES, """
                        - id: HEXAGONAL_MODULAR
                          label: Modular
                          description: Monólito modular.
                          templateDir: hexagonal-modular
                          modulesLabel: Módulos
                        - id: HEXAGONAL_LAYERED
                          label: Em camadas
                          description: Uma hexagonal só.
                          templateDir: hexagonal-layered
                          modulesLabel: Agregados
                        """)
                .with(PROFILES, profile("3.5", 17, 24, false, "web"))
                .with(DEPENDENCIES, """
                        - id: web
                          label: Web
                        """)
                .with(EXTRAS, """
                        - id: github-actions
                          requires: []
                        """);
    }

    static String profile(String line, int javaMin, int javaMaxTested, boolean legacy, String... artifactKeys) {
        String artifacts = Arrays.stream(artifactKeys)
                .map(key -> "    " + key + ": []\n")
                .collect(Collectors.joining());

        return """
                - line: "%s"
                  javaMin: %d
                  javaMaxTested: %d
                  legacy: %b
                  persistenceNamespace: jakarta
                  tests: SINGLE_STARTER
                  artifacts:
                %s""".formatted(line, javaMin, javaMaxTested, legacy, artifacts);
    }

    CatalogFixture with(String file, String content) {
        files.put(file, content);

        return this;
    }

    CatalogFixture without(String file) {
        files.remove(file);

        return this;
    }

    YamlCatalogRepository load(Path root) throws IOException {
        Path directory = Files.createDirectories(root.resolve(BASE_PATH));

        for (Map.Entry<String, String> file : files.entrySet()) {
            Files.writeString(directory.resolve(file.getKey()), file.getValue());
        }

        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            return new YamlCatalogRepository(loader, BASE_PATH);
        }
    }
}
