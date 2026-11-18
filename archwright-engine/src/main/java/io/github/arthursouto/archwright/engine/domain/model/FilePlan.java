package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record FilePlan(List<PlannedFile> files) {

    public FilePlan {
        files = List.copyOf(files);
    }

    public List<String> targets() {
        return files.stream().map(PlannedFile::target).toList();
    }
}
