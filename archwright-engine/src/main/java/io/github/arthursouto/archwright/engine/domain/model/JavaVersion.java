package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

public record JavaVersion(int value) implements Comparable<JavaVersion> {

    private static final Pattern NUMBER = Pattern.compile("\\d{1,3}");

    public JavaVersion {
        if (!isKnown(value)) {
            throw new IllegalArgumentException("unsupported java version: " + value);
        }
    }

    public static Optional<JavaVersion> parse(String raw) {
        String trimmed = raw.trim();

        if (!NUMBER.matcher(trimmed).matches()) {
            return Optional.empty();
        }

        int number = Integer.parseInt(trimmed);

        return isKnown(number) ? Optional.of(new JavaVersion(number)) : Optional.empty();
    }

    @Override
    public int compareTo(JavaVersion other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    private static boolean isKnown(int candidate) {
        return candidate == 8 || candidate == 11 || candidate == 17 || candidate >= 21;
    }
}
