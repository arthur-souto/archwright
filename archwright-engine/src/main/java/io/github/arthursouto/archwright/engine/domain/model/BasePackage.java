package io.github.arthursouto.archwright.engine.domain.model;

import io.github.arthursouto.archwright.engine.domain.naming.PackageSegments;

public record BasePackage(String value) {

    public BasePackage {
        if (PackageSegments.firstInvalid(value).isPresent()) {
            throw new IllegalArgumentException("invalid base package: " + value);
        }
    }
}
