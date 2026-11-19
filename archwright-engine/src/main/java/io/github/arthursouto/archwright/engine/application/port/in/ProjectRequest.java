package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.List;

public record ProjectRequest(
        String artifactId,
        String groupId,
        String basePackage,
        String description,
        String architectureStyle,
        List<String> modules,
        String bootVersion,
        String javaVersion,
        String kit,
        List<String> dependencies,
        List<String> extras,
        boolean fitnessFunctions) {
}
