package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Optional;

public record Description(String value) {

    public static final int MAX_LENGTH = 255;

    public Description {
        if (!isValid(value)) {
            throw new IllegalArgumentException("invalid description");
        }
    }

    public static Description empty() {
        return new Description("");
    }

    public static Optional<Description> parse(String raw) {
        return isValid(raw) ? Optional.of(new Description(raw)) : Optional.empty();
    }

    private static boolean isValid(String candidate) {
        return candidate.length() <= MAX_LENGTH
                && candidate.chars().noneMatch(character -> character == '\n' || character == '\r');
    }
}
