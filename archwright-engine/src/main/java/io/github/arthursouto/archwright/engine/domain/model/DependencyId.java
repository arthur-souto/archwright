package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

public record DependencyId(String value) {

    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    public DependencyId {
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid dependency id: " + value);
        }
    }

    public static Optional<DependencyId> parse(String raw) {
        String trimmed = raw.trim();

        return PATTERN.matcher(trimmed).matches() ? Optional.of(new DependencyId(trimmed)) : Optional.empty();
    }
}
