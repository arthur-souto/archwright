package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record ManifestFile(
        String template,
        String target,
        List<String> when,
        boolean executable,
        boolean render,
        LineEnding lineEnding,
        Repeat repeat) {

    public ManifestFile {
        when = List.copyOf(when);
    }
}
