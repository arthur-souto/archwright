package io.github.arthursouto.archwright.desktop.wizard;

import java.util.List;

public record Preset(String id, String architectureStyle, String kit, List<String> dependencies, List<String> extras) {

    private static final String MODULAR = "HEXAGONAL_MODULAR";
    private static final String LAYERED = "HEXAGONAL_LAYERED";
    private static final String BASIC = "BASIC";
    private static final String NONE = "NONE";

    public Preset {
        dependencies = List.copyOf(dependencies);
        extras = List.copyOf(extras);
    }

    public static List<Preset> all() {
        return List.of(
                new Preset("modular-basic", MODULAR, BASIC, basicDependencies(), List.of()),
                new Preset("layered-basic", LAYERED, BASIC, basicDependencies(), List.of()),
                new Preset("api-minimal", LAYERED, NONE, List.of("web", "validation", "actuator"), List.of()));
    }

    public void applyTo(ProjectForm form) {
        form.architectureStyle().set(architectureStyle);
        form.kit().set(kit);
        form.useDependencies(dependencies);
        form.extras().clear();
        form.extras().addAll(extras);
        form.fitnessFunctions().set(true);
    }

    private static List<String> basicDependencies() {
        return List.of("web", "validation", "actuator", "jpa", "flyway", "lombok");
    }
}
