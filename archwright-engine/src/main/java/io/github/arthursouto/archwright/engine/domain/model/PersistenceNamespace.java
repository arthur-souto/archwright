package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum PersistenceNamespace {
    JAVAX("javax"),
    JAKARTA("jakarta");

    private final String prefix;

    PersistenceNamespace(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    public static Optional<PersistenceNamespace> parse(String raw) {
        String trimmed = raw.trim();

        return Arrays.stream(values())
                .filter(namespace -> namespace.prefix.equals(trimmed))
                .findFirst();
    }
}
