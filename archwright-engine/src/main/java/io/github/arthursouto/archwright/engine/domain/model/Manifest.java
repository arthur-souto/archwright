package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record Manifest(List<ManifestFile> files, List<ManifestPackage> packages) {

    public Manifest {
        files = List.copyOf(files);
        packages = List.copyOf(packages);
    }
}
