package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Set;

public record BootProfile(
        String line,
        int javaMin,
        int javaMaxTested,
        boolean legacy,
        Set<DependencyId> availableDependencies) {

    public BootProfile {
        if (javaMin > javaMaxTested) {
            throw new IllegalArgumentException(
                    "boot profile " + line + ": javaMin " + javaMin + " is greater than javaMaxTested " + javaMaxTested);
        }

        availableDependencies = Set.copyOf(availableDependencies);
    }

    public boolean isAvailable(DependencyId dependency) {
        return availableDependencies.contains(dependency);
    }

    public boolean supports(JavaVersion java) {
        return java.value() >= javaMin;
    }

    public boolean isTested(JavaVersion java) {
        return java.value() <= javaMaxTested;
    }
}
