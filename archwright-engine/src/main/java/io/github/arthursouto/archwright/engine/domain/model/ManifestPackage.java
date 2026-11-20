package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record ManifestPackage(String name, String description, List<String> when, Repeat repeat) {

    public ManifestPackage {
        when = List.copyOf(when);
    }
}
