package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ExtraValidator {

    private static final String EXTRAS = "extras";

    private final Catalog catalog;

    ExtraValidator(Catalog catalog) {
        this.catalog = catalog;
    }

    Checked<List<ExtraId>> validate(ProjectDraft draft, Checked<List<DependencyId>> dependencies) {
        List<String> rawIds = draft.extras().stream().map(String::trim).distinct().toList();

        List<Violation> violations = rawIds.stream()
                .flatMap(raw -> violationsFor(raw, dependencies).stream())
                .toList();

        if (!violations.isEmpty()) {
            return Checked.invalid(violations);
        }

        return Checked.valid(rawIds.stream().map(ExtraId::new).toList());
    }

    private List<Violation> violationsFor(String raw, Checked<List<DependencyId>> dependencies) {
        Optional<ExtraDefinition> definition = ExtraId.parse(raw).flatMap(catalog::extra);

        if (definition.isEmpty()) {
            return List.of(Violation.of(ViolationCode.UNKNOWN_EXTRA, EXTRAS, "id", raw));
        }

        if (!dependencies.isValid()) {
            return List.of();
        }

        return missingRequirements(definition.get(), dependencies.value());
    }

    private List<Violation> missingRequirements(ExtraDefinition definition, List<DependencyId> available) {
        return definition.requires().stream()
                .filter(required -> !available.contains(required))
                .map(required -> new Violation(
                        ViolationCode.EXTRA_REQUIRES_DEPENDENCY,
                        EXTRAS,
                        Map.of("id", definition.id().value(), "requires", required.value())))
                .toList();
    }
}
