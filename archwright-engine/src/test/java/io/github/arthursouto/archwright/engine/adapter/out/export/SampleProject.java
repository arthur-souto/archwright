package io.github.arthursouto.archwright.engine.adapter.out.export;

import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;

import java.nio.charset.StandardCharsets;
import java.util.List;

final class SampleProject {

    private SampleProject() {
    }

    static GeneratedProject pedidoApi() {
        return new GeneratedProject("pedido-api", List.of(
                file("mvnw", "#!/bin/sh\necho wrapper\n", true),
                file("pom.xml", "<project/>\n", false),
                file("src/main/java/com/acme/App.java", "class App {}\n", false),
                file("src/main/resources/application.yaml", "server:\n  port: 8080\n", false)));
    }

    static GeneratedProject withFiles(GeneratedFile... files) {
        return new GeneratedProject("pedido-api", List.of(files));
    }

    static GeneratedFile file(String path, String content, boolean executable) {
        return new GeneratedFile(path, content.getBytes(StandardCharsets.UTF_8), executable);
    }
}
