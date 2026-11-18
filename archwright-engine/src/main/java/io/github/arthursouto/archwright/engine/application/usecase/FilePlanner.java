package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.out.TemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.LineEnding;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.model.ManifestFile;
import io.github.arthursouto.archwright.engine.domain.model.ManifestPackage;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.Repeat;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class FilePlanner {

    private static final String COMMON_DIRECTORY = "common";
    private static final String PACKAGE_INFO_TEMPLATE = "common/package-info.java.mustache";
    private static final String JAVA_SOURCES = "src/main/java/";
    private static final String NEGATION = "!";

    private final TemplateRepository repository;
    private final TemplateRenderer renderer;
    private final List<ArchitectureStyleDefinition> styles;

    public FilePlanner(
            TemplateRepository repository, TemplateRenderer renderer, List<ArchitectureStyleDefinition> styles) {
        this.repository = repository;
        this.renderer = renderer;
        this.styles = List.copyOf(styles);
    }

    public FilePlan plan(ProjectSpec spec, TemplateContext context) {
        List<PlannedFile> planned = Stream.of(COMMON_DIRECTORY, styleDirectory(spec))
                .flatMap(directory -> planDirectory(directory, context).stream())
                .toList();

        requireUniqueTargets(planned);

        return new FilePlan(planned.stream().sorted(Comparator.comparing(PlannedFile::target)).toList());
    }

    private String styleDirectory(ProjectSpec spec) {
        return styles.stream()
                .filter(definition -> definition.style() == spec.architecture().style())
                .findFirst()
                .map(ArchitectureStyleDefinition::templateDir)
                .orElseThrow(() -> new IllegalStateException("no style definition for " + spec.architecture().style()));
    }

    private List<PlannedFile> planDirectory(String directory, TemplateContext context) {
        Manifest manifest = repository.manifest(directory);

        Stream<PlannedFile> files = manifest.files().stream()
                .flatMap(file -> planFile(directory, file, context).stream());
        Stream<PlannedFile> packages = manifest.packages().stream()
                .flatMap(entry -> planPackage(entry, context).stream());

        return Stream.concat(files, packages).toList();
    }

    private List<PlannedFile> planFile(String directory, ManifestFile file, TemplateContext context) {
        if (!satisfied(directory, file.when(), context)) {
            return List.of();
        }

        return scopes(file.repeat(), context).stream()
                .map(scope -> plannedFile(directory, file, scope))
                .toList();
    }

    private PlannedFile plannedFile(String directory, ManifestFile file, Map<String, Object> scope) {
        String target = validated(renderer.render(file.target(), scope));

        return new PlannedFile(
                directory + "/" + file.template(), target, file.executable(), file.lineEnding(), file.render(), Map.of());
    }

    private List<PlannedFile> planPackage(ManifestPackage entry, TemplateContext context) {
        return scopes(entry.repeat(), context).stream()
                .map(scope -> packageInfo(entry, context.text("basePackage"), scope))
                .toList();
    }

    private PlannedFile packageInfo(ManifestPackage entry, String basePackage, Map<String, Object> scope) {
        String relative = renderer.render(entry.name(), scope);
        String fullPackage = relative.isEmpty() ? basePackage : basePackage + "." + relative;
        String target = validated(JAVA_SOURCES + fullPackage.replace('.', '/') + "/package-info.java");
        Map<String, String> variables = Map.of(
                "package", fullPackage,
                "description", renderer.render(entry.description(), scope));

        return new PlannedFile(PACKAGE_INFO_TEMPLATE, target, false, LineEnding.LF, true, variables);
    }

    private List<Map<String, Object>> scopes(Repeat repeat, TemplateContext context) {
        if (!repeat.repeats()) {
            return List.of(context.values());
        }

        return context.sections(repeat.listName()).stream()
                .map(item -> scopeWith(context, repeat.variable(), item))
                .toList();
    }

    private Map<String, Object> scopeWith(TemplateContext context, String variable, TemplateContext item) {
        Map<String, Object> scope = new LinkedHashMap<>(context.values());
        scope.put(variable, item.values());

        return scope;
    }

    private boolean satisfied(String directory, List<String> conditions, TemplateContext context) {
        return conditions.stream().allMatch(condition -> holds(directory, condition, context));
    }

    private boolean holds(String directory, String condition, TemplateContext context) {
        boolean negated = condition.startsWith(NEGATION);
        String flag = negated ? condition.substring(NEGATION.length()) : condition;

        try {
            return negated != context.flag(flag);
        } catch (IllegalArgumentException unknownFlag) {
            throw new IllegalStateException(
                    "template: " + directory + "/manifest.yaml: unknown flag '" + flag + "' in when", unknownFlag);
        }
    }

    private String validated(String target) {
        boolean invalid = target.isBlank()
                || target.startsWith("/")
                || target.contains("\\")
                || target.contains(":")
                || Arrays.stream(target.split("/", -1)).anyMatch(this::isForbiddenSegment);

        if (invalid) {
            throw new IllegalStateException("INVALID_TEMPLATE_PATH: " + target);
        }

        return target;
    }

    private boolean isForbiddenSegment(String segment) {
        return segment.isEmpty() || segment.equals(".") || segment.equals("..");
    }

    private void requireUniqueTargets(List<PlannedFile> planned) {
        Set<String> seen = new HashSet<>();

        planned.stream()
                .map(PlannedFile::target)
                .filter(target -> !seen.add(target))
                .findFirst()
                .ifPresent(duplicated -> {
                    throw new IllegalStateException("duplicated target in the file plan: " + duplicated);
                });
    }
}
