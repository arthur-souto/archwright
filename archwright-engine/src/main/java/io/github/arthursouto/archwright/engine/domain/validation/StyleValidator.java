package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.Architecture;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.ModuleName;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyleStrategy;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

final class StyleValidator {

    private static final String ARCHITECTURE_STYLE = "architectureStyle";
    private static final String MODULES = "modules";
    private static final int MAX_MODULES = 20;

    private final ArchitectureStyles styles;

    StyleValidator(ArchitectureStyles styles) {
        this.styles = styles;
    }

    Checked<Architecture> validate(ProjectDraft draft) {
        Optional<ArchitectureStyle> style = ArchitectureStyle.parse(draft.architectureStyle());

        if (style.isEmpty()) {
            return Checked.invalid(Violation.of(
                    ViolationCode.UNKNOWN_ARCHITECTURE_STYLE, ARCHITECTURE_STYLE, "value", draft.architectureStyle()));
        }

        return checkModules(style.get(), draft.modules());
    }

    private Checked<Architecture> checkModules(ArchitectureStyle style, List<String> rawNames) {
        ArchitectureStyleStrategy strategy = styles.strategyFor(style);

        List<Violation> violations = Stream.concat(
                        countViolation(rawNames.size(), strategy).stream(),
                        nameViolations(rawNames, strategy.reservedModuleNames()).stream())
                .toList();

        if (!violations.isEmpty()) {
            return Checked.invalid(violations);
        }

        return Checked.valid(new Architecture(style, parseAll(rawNames)));
    }

    private Optional<Violation> countViolation(int count, ArchitectureStyleStrategy strategy) {
        if (strategy.modulesRequired() && count == 0) {
            return Optional.of(Violation.of(ViolationCode.MODULES_REQUIRED, MODULES));
        }

        if (count > MAX_MODULES) {
            return Optional.of(Violation.of(
                    ViolationCode.TOO_MANY_MODULES, MODULES, "max", String.valueOf(MAX_MODULES)));
        }

        return Optional.empty();
    }

    private List<Violation> nameViolations(List<String> rawNames, Set<String> reservedNames) {
        Set<String> seen = new HashSet<>();
        List<Violation> violations = new ArrayList<>();

        for (String raw : rawNames) {
            nameViolation(raw, reservedNames, seen).ifPresent(violations::add);
        }

        return violations;
    }

    private Optional<Violation> nameViolation(String raw, Set<String> reservedNames, Set<String> seen) {
        Optional<ModuleName> parsed = ModuleName.parse(raw);

        if (parsed.isEmpty()) {
            return Optional.of(moduleViolation(ViolationCode.INVALID_MODULE_NAME, raw.trim()));
        }

        String name = parsed.get().value();

        if (reservedNames.contains(name)) {
            return Optional.of(moduleViolation(ViolationCode.RESERVED_MODULE_NAME, name));
        }

        if (!seen.add(name)) {
            return Optional.of(moduleViolation(ViolationCode.DUPLICATED_MODULE_NAME, name));
        }

        return Optional.empty();
    }

    private List<ModuleName> parseAll(List<String> rawNames) {
        return rawNames.stream()
                .map(ModuleName::parse)
                .flatMap(Optional::stream)
                .toList();
    }

    private Violation moduleViolation(ViolationCode code, String name) {
        return Violation.of(code, MODULES, "name", name);
    }
}
