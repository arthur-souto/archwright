package io.github.arthursouto.archwright.engine.domain.validation;

import java.util.Map;

public record Violation(ViolationCode code, String field, Map<String, String> params) {

    public Violation {
        params = Map.copyOf(params);
    }

    public static Violation of(ViolationCode code, String field) {
        return new Violation(code, field, Map.of());
    }

    public static Violation of(ViolationCode code, String field, String paramName, String paramValue) {
        return new Violation(code, field, Map.of(paramName, paramValue));
    }
}
