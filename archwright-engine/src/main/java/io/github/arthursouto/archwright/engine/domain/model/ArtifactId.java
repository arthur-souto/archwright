package io.github.arthursouto.archwright.engine.domain.model;

import io.github.arthursouto.archwright.engine.domain.naming.Names;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record ArtifactId(String value) {

    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9]*([.-][a-z0-9]+)*");
    private static final int MAX_LENGTH = 64;

    public ArtifactId {
        if (!isValid(value)) {
            throw new IllegalArgumentException("invalid artifactId: " + value);
        }
    }

    public static Optional<ArtifactId> parse(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);

        return isValid(normalized) ? Optional.of(new ArtifactId(normalized)) : Optional.empty();
    }

    public String mainClassName() {
        return Names.mainClassName(value);
    }

    private static boolean isValid(String candidate) {
        return candidate.length() <= MAX_LENGTH && PATTERN.matcher(candidate).matches();
    }
}
