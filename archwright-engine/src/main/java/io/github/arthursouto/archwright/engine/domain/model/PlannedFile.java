package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Map;

public record PlannedFile(
        String template,
        String target,
        boolean executable,
        LineEnding lineEnding,
        boolean render,
        Map<String, String> variables) {

    public PlannedFile {
        variables = Map.copyOf(variables);
    }
}
