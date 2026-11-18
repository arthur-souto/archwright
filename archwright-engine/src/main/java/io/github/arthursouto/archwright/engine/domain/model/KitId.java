package io.github.arthursouto.archwright.engine.domain.model;

public record KitId(String value) {

    public KitId {
        if (value.isBlank()) {
            throw new IllegalArgumentException("kit id must not be blank");
        }
    }
}
