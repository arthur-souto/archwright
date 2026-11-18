package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record GeneratedProject(String artifactId, List<GeneratedFile> files) {

    public GeneratedProject {
        Objects.requireNonNull(artifactId, "artifactId");
        files = List.copyOf(files);
    }

    public int fileCount() {
        return files.size();
    }

    public long totalBytes() {
        return files.stream().mapToLong(GeneratedFile::size).sum();
    }

    public Optional<GeneratedFile> find(String path) {
        return files.stream().filter(file -> file.path().equals(path)).findFirst();
    }
}
