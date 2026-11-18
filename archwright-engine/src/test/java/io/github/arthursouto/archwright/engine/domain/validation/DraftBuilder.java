package io.github.arthursouto.archwright.engine.domain.validation;

import java.util.List;

final class DraftBuilder {

    private String artifactId = "pedido-api";
    private String groupId = "com.acme";
    private String style = "HEXAGONAL_MODULAR";
    private List<String> modules = List.of("pedidos", "pagamentos");
    private String bootVersion = "3.5.3";
    private String javaVersion = "21";
    private String kit = "BASIC";
    private List<String> dependencies = List.of("web", "validation", "actuator", "jpa", "flyway", "lombok");
    private List<String> extras = List.of();

    static DraftBuilder valid() {
        return new DraftBuilder();
    }

    DraftBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    DraftBuilder style(String style) {
        this.style = style;
        return this;
    }

    DraftBuilder modules(String... modules) {
        this.modules = List.of(modules);
        return this;
    }

    DraftBuilder boot(String bootVersion) {
        this.bootVersion = bootVersion;
        return this;
    }

    DraftBuilder java(String javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    DraftBuilder kit(String kit) {
        this.kit = kit;
        return this;
    }

    DraftBuilder dependencies(String... dependencies) {
        this.dependencies = List.of(dependencies);
        return this;
    }

    DraftBuilder extras(String... extras) {
        this.extras = List.of(extras);
        return this;
    }

    ProjectDraft build() {
        return new ProjectDraft(
                artifactId, groupId, "", "API de pedidos",
                style, modules,
                bootVersion, javaVersion,
                kit, dependencies, extras, true);
    }
}
