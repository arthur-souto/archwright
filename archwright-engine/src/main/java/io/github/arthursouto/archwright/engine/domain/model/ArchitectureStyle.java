package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum ArchitectureStyle {
    HEXAGONAL_MODULAR,
    HEXAGONAL_LAYERED;

    public static Optional<ArchitectureStyle> parse(String raw) {
        String id = raw.trim();

        return Arrays.stream(values())
                .filter(style -> style.name().equals(id))
                .findFirst();
    }
}
