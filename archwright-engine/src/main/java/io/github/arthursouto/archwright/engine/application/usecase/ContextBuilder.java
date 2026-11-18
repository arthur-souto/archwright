package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.domain.kit.KitStrategy;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.JavaVersion;
import io.github.arthursouto.archwright.engine.domain.model.ModuleName;
import io.github.arthursouto.archwright.engine.domain.model.ProjectIdentity;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import io.github.arthursouto.archwright.engine.domain.model.TestStrategy;
import io.github.arthursouto.archwright.engine.domain.naming.Escaping;
import io.github.arthursouto.archwright.engine.domain.naming.Names;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ContextBuilder {

    private static final DependencyId JPA = new DependencyId("jpa");

    private final ProfileResolver resolver;
    private final Catalog catalog;
    private final List<ArchitectureStyleDefinition> styles;
    private final Kits kits;
    private final DependencySection dependencySection;

    public ContextBuilder(
            ProfileResolver resolver, Catalog catalog, List<ArchitectureStyleDefinition> styles, Kits kits) {
        this.resolver = resolver;
        this.catalog = catalog;
        this.styles = List.copyOf(styles);
        this.kits = kits;
        this.dependencySection = new DependencySection(catalog);
    }

    public TemplateContext build(ProjectSpec spec) {
        BootProfile profile = resolver.profileFor(spec.bootVersion());

        Map<String, Object> context = new LinkedHashMap<>();
        context.putAll(identity(spec.identity()));
        context.put("style", style(spec.architecture().style()));
        context.put("boot", boot(spec.bootVersion()));
        context.put("java", java(spec.javaVersion()));
        context.put("kit", kit(spec));
        context.put("tests", Map.of("perTechnology", profile.testStrategy() == TestStrategy.PER_TECHNOLOGY));
        context.put("persistence", Map.of("namespace", profile.persistenceNamespace().prefix()));
        context.put("fitness", Map.of("enabled", spec.fitnessFunctions()));
        context.putAll(dependencySection.describe(spec, profile));
        context.putAll(modules(spec));
        context.put("dep", dependencyFlags(spec));
        context.put("extra", extraFlags(spec));

        if (spec.dependencies().contains(JPA)) {
            context.put("database", Map.of("name", Names.databaseName(spec.identity().artifactId().value())));
        }

        return new TemplateContext(context);
    }

    private Map<String, Object> identity(ProjectIdentity identity) {
        String description = identity.description().value();
        String basePackage = identity.basePackage().value();

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("artifactId", identity.artifactId().value());
        values.put("groupId", identity.groupId().value());
        values.put("basePackage", basePackage);
        values.put("basePackagePath", basePackage.replace('.', '/'));
        values.put("applicationClass", identity.artifactId().mainClassName());
        values.put("descriptionXml", Escaping.xml(description));
        values.put("descriptionMarkdown", Escaping.markdown(description));
        values.put("hasDescription", !description.isEmpty());

        return values;
    }

    private Map<String, Object> style(ArchitectureStyle style) {
        ArchitectureStyleDefinition definition = styles.stream()
                .filter(candidate -> candidate.style() == style)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no style definition for " + style));

        return Map.of(
                "id", style.name(),
                "label", definition.label(),
                "modular", style == ArchitectureStyle.HEXAGONAL_MODULAR,
                "layered", style == ArchitectureStyle.HEXAGONAL_LAYERED);
    }

    private Map<String, Object> boot(BootVersion version) {
        return Map.of(
                "version", version.toString(),
                "line", version.line(),
                "v2", version.major() == 2,
                "v3", version.major() == 3,
                "v4", version.major() == 4);
    }

    private Map<String, Object> java(JavaVersion version) {
        String property = version.value() == 8 ? "1.8" : version.toString();

        return Map.of("version", version.toString(), "property", property);
    }

    private Map<String, Object> kit(ProjectSpec spec) {
        KitStrategy kit = kits.find(spec.kit().value())
                .orElseThrow(() -> new IllegalStateException("no kit strategy for " + spec.kit().value()));

        return Map.of("id", kit.id().value(), "label", kit.label());
    }

    private Map<String, Object> modules(ProjectSpec spec) {
        String basePackage = spec.identity().basePackage().value();
        List<Map<String, Object>> views = spec.architecture().modules().stream()
                .map(module -> moduleView(basePackage, module))
                .toList();

        boolean modular = spec.architecture().style() == ArchitectureStyle.HEXAGONAL_MODULAR;

        return Map.of(
                "modules", modular ? views : List.of(),
                "aggregates", modular ? List.of() : views);
    }

    private Map<String, Object> moduleView(String basePackage, ModuleName module) {
        String name = module.value();

        return Map.of(
                "name", name,
                "packageName", basePackage + "." + name,
                "packagePath", basePackage.replace('.', '/') + "/" + name,
                "displayName", Names.displayName(name));
    }

    private Map<String, Boolean> dependencyFlags(ProjectSpec spec) {
        return catalog.dependencies().stream()
                .map(DependencyDefinition::id)
                .collect(Collectors.toMap(DependencyId::value, spec.dependencies()::contains));
    }

    private Map<String, Boolean> extraFlags(ProjectSpec spec) {
        return catalog.extras().stream()
                .collect(Collectors.toMap(extra -> extra.id().value(), extra -> spec.extras().contains(extra.id())));
    }
}
