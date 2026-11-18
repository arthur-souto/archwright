package io.github.arthursouto.archwright.engine.domain.model;

import io.github.arthursouto.archwright.engine.domain.naming.PackageSegments;

import java.util.Locale;
import java.util.Optional;

public record ModuleName(String value) {

    public ModuleName {
        if (!PackageSegments.isValid(value)) {
            throw new IllegalArgumentException("invalid module name: " + value);
        }
    }

    public static Optional<ModuleName> parse(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);

        return PackageSegments.isValid(normalized) ? Optional.of(new ModuleName(normalized)) : Optional.empty();
    }
}
