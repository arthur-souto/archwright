package io.github.arthursouto.archwright.engine.application.port.in;

import io.github.arthursouto.archwright.engine.domain.validation.ProjectDraft;

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

    public ProjectDraft toDraft() {
        return new ProjectDraft(
                artifactId,
                groupId,
                basePackage,
                description,
                architectureStyle,
                modules,
                bootVersion,
                javaVersion,
                kit,
                dependencies,
                extras,
                fitnessFunctions);
    }
}
