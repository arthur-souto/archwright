package io.github.arthursouto.archwright.desktop.wizard;

import java.util.Optional;
import java.util.Set;

public enum WizardStep {

    PROJECT("artifactId", "groupId", "basePackage", "description", "architectureStyle", "modules"),
    VERSIONS("bootVersion", "javaVersion"),
    DEPENDENCIES("kit", "dependencies", "extras"),
    EXPORT("destination");

    private final Set<String> fields;

    WizardStep(String... fields) {
        this.fields = Set.of(fields);
    }

    public boolean owns(String field) {
        return fields.contains(field);
    }

    public static Optional<WizardStep> owning(String field) {
        for (WizardStep step : values()) {
            if (step.owns(field)) {
                return Optional.of(step);
            }
        }

        return Optional.empty();
    }

    public boolean isFirst() {
        return ordinal() == 0;
    }

    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    public WizardStep next() {
        return isLast() ? this : values()[ordinal() + 1];
    }

    public WizardStep previous() {
        return isFirst() ? this : values()[ordinal() - 1];
    }
}
