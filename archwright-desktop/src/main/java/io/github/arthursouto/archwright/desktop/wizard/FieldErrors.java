package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FieldErrors {

    private static final FieldErrors NONE = new FieldErrors(Map.of());

    private final Map<String, List<String>> byField;

    private FieldErrors(Map<String, List<String>> byField) {
        this.byField = byField;
    }

    public static FieldErrors none() {
        return NONE;
    }

    public static FieldErrors from(List<Violation> violations, Messages messages) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        for (Violation violation : violations) {
            grouped.computeIfAbsent(violation.field(), field -> new ArrayList<>()).add(messages.violation(violation));
        }

        return new FieldErrors(grouped);
    }

    public List<String> forField(String field) {
        return List.copyOf(byField.getOrDefault(field, List.of()));
    }

    public String messageFor(String field) {
        return String.join("\n", forField(field));
    }

    public boolean hasErrorIn(WizardStep step) {
        return byField.keySet().stream().anyMatch(step::owns);
    }

    public boolean isEmpty() {
        return byField.isEmpty();
    }
}
