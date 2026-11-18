package io.github.arthursouto.archwright.engine.domain.validation;

import java.util.Map;

public record Warning(WarningCode code, Map<String, String> params) {

    public Warning {
        params = Map.copyOf(params);
    }
}
