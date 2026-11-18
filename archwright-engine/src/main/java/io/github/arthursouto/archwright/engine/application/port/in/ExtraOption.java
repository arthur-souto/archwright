package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.List;

public record ExtraOption(String id, String label, List<String> requires) {

    public ExtraOption {
        requires = List.copyOf(requires);
    }
}
