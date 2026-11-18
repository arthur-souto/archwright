package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MavenCoordinate(String groupId, String artifactId, String version, String scope) {

    private static final Pattern PROPERTY_REFERENCE = Pattern.compile("\\$\\{([^}]+)}");

    public MavenCoordinate {
        requireText(groupId, "groupId");
        requireText(artifactId, "artifactId");
        requireTextWhenPresent(version, "version");
        requireTextWhenPresent(scope, "scope");
    }

    public static MavenCoordinate of(String groupId, String artifactId) {
        return new MavenCoordinate(groupId, artifactId, null, null);
    }

    public Optional<String> versionProperty() {
        if (version == null) {
            return Optional.empty();
        }

        Matcher matcher = PROPERTY_REFERENCE.matcher(version);

        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("maven coordinate " + name + " must not be blank");
        }
    }

    private static void requireTextWhenPresent(String value, String name) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("maven coordinate " + name + " must not be blank when present");
        }
    }
}
