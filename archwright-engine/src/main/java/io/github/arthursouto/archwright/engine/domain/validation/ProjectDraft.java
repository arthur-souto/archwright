package io.github.arthursouto.archwright.engine.domain.validation;

import java.util.List;

public record ProjectDraft(
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

    public ProjectDraft {
        artifactId = textOrEmpty(artifactId);
        groupId = textOrEmpty(groupId);
        basePackage = textOrEmpty(basePackage);
        description = textOrEmpty(description);
        architectureStyle = textOrEmpty(architectureStyle);
        modules = textsOrEmpty(modules);
        bootVersion = textOrEmpty(bootVersion);
        javaVersion = textOrEmpty(javaVersion);
        kit = textOrEmpty(kit);
        dependencies = textsOrEmpty(dependencies);
        extras = textsOrEmpty(extras);
    }

    private static String textOrEmpty(String text) {
        return text == null ? "" : text;
    }

    private static List<String> textsOrEmpty(List<String> texts) {
        return texts == null ? List.of() : texts.stream().map(ProjectDraft::textOrEmpty).toList();
    }
}
