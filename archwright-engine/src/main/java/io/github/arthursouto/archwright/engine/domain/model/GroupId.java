package io.github.arthursouto.archwright.engine.domain.model;

import io.github.arthursouto.archwright.engine.domain.naming.PackageSegments;

import java.util.Optional;

public record GroupId(String value) {

    private static final int MAX_LENGTH = 128;

    public GroupId {
        if (!isValid(value)) {
            throw new IllegalArgumentException("invalid groupId: " + value);
        }
    }

    public static Optional<GroupId> parse(String raw) {
        String trimmed = raw.trim();

        return isValid(trimmed) ? Optional.of(new GroupId(trimmed)) : Optional.empty();
    }

    private static boolean isValid(String candidate) {
        return candidate.length() <= MAX_LENGTH
                && PackageSegments.split(candidate).stream().allMatch(PackageSegments::hasIdentifierShape);
    }
}
