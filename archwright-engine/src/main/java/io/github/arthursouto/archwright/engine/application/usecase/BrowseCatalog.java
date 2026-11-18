package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.DependencyOption;
import io.github.arthursouto.archwright.engine.application.port.in.ExtraOption;
import io.github.arthursouto.archwright.engine.application.port.in.FitnessRuleSummary;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.domain.kit.KitStrategy;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.FitnessRuleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.JavaVersion;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import io.github.arthursouto.archwright.engine.domain.naming.Names;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class BrowseCatalog implements CatalogQuery {

    private static final List<Integer> LEGACY_JAVA_VERSIONS = List.of(8, 11, 17);
    private static final List<Integer> LTS_JAVA_VERSIONS = List.of(8, 11, 17, 21, 25);
    private static final int FIRST_MODERN_JAVA = 21;
    private static final int NEWEST_KNOWN_JAVA = 25;

    private final Catalog catalog;
    private final List<ArchitectureStyleDefinition> styleDefinitions;
    private final ArchitectureStyles styles;
    private final Kits kits;
    private final BootVersionLookup versionLookup;

    public BrowseCatalog(
            Catalog catalog,
            List<ArchitectureStyleDefinition> styleDefinitions,
            ArchitectureStyles styles,
            Kits kits,
            BootVersionLookup versionLookup) {
        this.catalog = catalog;
        this.styleDefinitions = List.copyOf(styleDefinitions);
        this.styles = styles;
        this.kits = kits;
        this.versionLookup = versionLookup;
    }

    @Override
    public List<ArchitectureStyleOption> architectureStyles() {
        return styleDefinitions.stream().map(this::styleOption).toList();
    }

    @Override
    public BootVersions bootVersions() {
        return versionLookup.lookup();
    }

    @Override
    public List<JavaVersionOption> javaVersions(String bootVersion) {
        return profileOf(bootVersion).map(this::javaOptions).orElse(List.of());
    }

    @Override
    public List<KitOption> kits() {
        return kits.all().stream().map(this::kitOption).toList();
    }

    @Override
    public List<DependencyOption> dependencies(String bootVersion) {
        return profileOf(bootVersion).map(this::dependencyOptions).orElse(List.of());
    }

    @Override
    public List<ExtraOption> extras() {
        return catalog.extras().stream().map(this::extraOption).toList();
    }

    @Override
    public List<FitnessRuleSummary> fitnessRules(String style, List<String> dependencies, String bootVersion) {
        Optional<ArchitectureStyle> parsedStyle = ArchitectureStyle.parse(style == null ? "" : style);

        if (parsedStyle.isEmpty() || profileOf(bootVersion).isEmpty()) {
            return List.of();
        }

        TemplateContext flags = selectionFlags(parsedStyle.get(), dependencies == null ? List.of() : dependencies);

        return catalog.fitnessRules().stream()
                .filter(definition -> definition.appliesTo(flags))
                .flatMap(definition -> summaries(definition).stream())
                .toList();
    }

    @Override
    public String suggestBasePackage(String groupId, String artifactId) {
        return Names.suggestBasePackage(groupId == null ? "" : groupId, artifactId == null ? "" : artifactId);
    }

    private TemplateContext selectionFlags(ArchitectureStyle style, List<String> selected) {
        Map<String, Boolean> dependencyFlags = catalog.dependencies().stream()
                .collect(Collectors.toMap(definition -> definition.id().value(), definition -> selected.contains(definition.id().value())));

        return new TemplateContext(Map.of(
                "style", Map.of(
                        "modular", style == ArchitectureStyle.HEXAGONAL_MODULAR,
                        "layered", style == ArchitectureStyle.HEXAGONAL_LAYERED),
                "dep", dependencyFlags,
                "fitness", Map.of("enabled", true)));
    }

    private List<FitnessRuleSummary> summaries(FitnessRuleDefinition definition) {
        return definition.rules().stream()
                .map(rule -> new FitnessRuleSummary(rule.id(), definition.testClass(), rule.description()))
                .toList();
    }

    private Optional<BootProfile> profileOf(String bootVersion) {
        return BootVersion.parse(bootVersion == null ? "" : bootVersion).flatMap(catalog::profileFor);
    }

    private ArchitectureStyleOption styleOption(ArchitectureStyleDefinition definition) {
        return new ArchitectureStyleOption(
                definition.style().name(),
                definition.label(),
                definition.description(),
                definition.modulesLabel(),
                styles.strategyFor(definition.style()).modulesRequired());
    }

    private List<JavaVersionOption> javaOptions(BootProfile profile) {
        List<Integer> supported = candidates(profile).stream()
                .filter(version -> profile.supports(new JavaVersion(version)))
                .toList();
        int defaultVersion = defaultVersion(profile, supported);

        return supported.stream()
                .map(version -> javaOption(profile, version, defaultVersion))
                .toList();
    }

    private List<Integer> candidates(BootProfile profile) {
        int newest = Math.max(profile.javaMaxTested(), NEWEST_KNOWN_JAVA);

        return Stream.concat(
                        LEGACY_JAVA_VERSIONS.stream(),
                        IntStream.rangeClosed(FIRST_MODERN_JAVA, newest).boxed())
                .toList();
    }

    private int defaultVersion(BootProfile profile, List<Integer> supported) {
        return supported.stream()
                .filter(version -> LTS_JAVA_VERSIONS.contains(version) && profile.isTested(new JavaVersion(version)))
                .max(Integer::compare)
                .orElse(-1);
    }

    private JavaVersionOption javaOption(BootProfile profile, int version, int defaultVersion) {
        boolean aboveTested = !profile.isTested(new JavaVersion(version));

        return new JavaVersionOption(
                String.valueOf(version), LTS_JAVA_VERSIONS.contains(version), aboveTested, version == defaultVersion);
    }

    private KitOption kitOption(KitStrategy kit) {
        return new KitOption(kit.id().value(), kit.label(), kit.defaultDependencies().stream().map(DependencyId::value).toList());
    }

    private List<DependencyOption> dependencyOptions(BootProfile profile) {
        return catalog.dependencies().stream()
                .filter(definition -> profile.isAvailable(definition.id()))
                .map(this::dependencyOption)
                .toList();
    }

    private DependencyOption dependencyOption(DependencyDefinition definition) {
        return new DependencyOption(
                definition.id().value(),
                definition.label(),
                definition.description(),
                definition.group(),
                definition.implies().stream().map(DependencyId::value).toList());
    }

    private ExtraOption extraOption(ExtraDefinition definition) {
        return new ExtraOption(
                definition.id().value(),
                definition.label(),
                definition.requires().stream().map(DependencyId::value).toList());
    }
}
