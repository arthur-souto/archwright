package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum Repeat {
    NONE("", ""),
    MODULES("modules", "module"),
    AGGREGATES("aggregates", "aggregate");

    private final String listName;
    private final String variable;

    Repeat(String listName, String variable) {
        this.listName = listName;
        this.variable = variable;
    }

    public boolean repeats() {
        return this != NONE;
    }

    public String listName() {
        return listName;
    }

    public String variable() {
        return variable;
    }

    public static Optional<Repeat> parse(String raw) {
        String id = raw.trim();

        return Arrays.stream(values())
                .filter(Repeat::repeats)
                .filter(repeat -> repeat.listName.equals(id))
                .findFirst();
    }
}
