package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.Map;

public record Violation(String code, String field, Map<String, String> params) {

    public Violation {
        params = Map.copyOf(params);
    }
}
