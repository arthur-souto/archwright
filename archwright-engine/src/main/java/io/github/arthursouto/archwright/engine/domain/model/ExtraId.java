package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

public record ExtraId(String value) {

    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    public ExtraId {
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid extra id: " + value);
        }
    }

    public static Optional<ExtraId> parse(String raw) {
        String trimmed = raw.trim();

        return PATTERN.matcher(trimmed).matches() ? Optional.of(new ExtraId(trimmed)) : Optional.empty();
    }
}
