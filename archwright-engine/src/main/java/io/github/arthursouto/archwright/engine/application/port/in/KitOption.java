package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.List;

public record KitOption(String id, String label, List<String> dependencies) {

    public KitOption {
        dependencies = List.copyOf(dependencies);
    }
}
