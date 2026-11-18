package io.github.arthursouto.archwright.engine.domain.model;

public record FitnessRule(String id, String description) {

    public FitnessRule {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("fitness rule id must not be blank");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("fitness rule " + id + ": description must not be blank");
        }
    }
}
