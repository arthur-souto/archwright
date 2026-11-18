package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.catalog.YamlCatalogRepository;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import io.github.arthursouto.archwright.engine.domain.validation.ProjectDraft;
import io.github.arthursouto.archwright.engine.domain.validation.SpecValidator;
import io.github.arthursouto.archwright.engine.domain.validation.ValidationResult;

import java.util.List;

final class SpecBuilder {

    private static final YamlCatalogRepository REPOSITORY = new YamlCatalogRepository();
    private static final Catalog CATALOG =
            new Catalog(REPOSITORY.bootProfiles(), REPOSITORY.dependencies(), REPOSITORY.extras());
    private static final SpecValidator VALIDATOR =
            new SpecValidator(CATALOG, ArchitectureStyles.standard(), Kits.standard());

    private String artifactId = "pedido-api";
    private String description = "";
    private String style = "HEXAGONAL_MODULAR";
    private List<String> modules = List.of("pedidos", "pagamentos");
    private String boot = "4.1.2";
    private String java = "25";
    private String kit = "BASIC";
    private List<String> dependencies = List.of("web", "validation", "actuator", "jpa", "flyway", "lombok");
    private List<String> extras = List.of();
    private boolean fitness = true;

    static SpecBuilder basic() {
        return new SpecBuilder();
    }

    static Catalog catalog() {
        return CATALOG;
    }

    static YamlCatalogRepository repository() {
        return REPOSITORY;
    }

    SpecBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    SpecBuilder description(String description) {
        this.description = description;
        return this;
    }

    SpecBuilder layered(String... aggregates) {
        this.style = "HEXAGONAL_LAYERED";
        this.modules = List.of(aggregates);
        return this;
    }

    SpecBuilder modules(String... modules) {
        this.modules = List.of(modules);
        return this;
    }

    SpecBuilder boot(String boot, String java) {
        this.boot = boot;
        this.java = java;
        return this;
    }

    SpecBuilder noKit() {
        this.kit = "NONE";
        this.dependencies = List.of();
        return this;
    }

    SpecBuilder dependencies(String... dependencies) {
        this.dependencies = List.of(dependencies);
        return this;
    }

    SpecBuilder extras(String... extras) {
        this.extras = List.of(extras);
        return this;
    }

    SpecBuilder fitness(boolean fitness) {
        this.fitness = fitness;
        return this;
    }

    ProjectSpec build() {
        ProjectDraft draft = new ProjectDraft(
                artifactId, "com.acme", "", description,
                style, modules,
                boot, java,
                kit, dependencies, extras, fitness);

        ValidationResult result = VALIDATOR.validate(draft);

        if (!(result instanceof ValidationResult.Valid valid)) {
            throw new AssertionError("test spec is invalid: " + result);
        }

        return valid.spec();
    }
}
