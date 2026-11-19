package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.Map;

public record Warning(String code, Map<String, String> params) {

    public Warning {
        params = Map.copyOf(params);
    }
}
