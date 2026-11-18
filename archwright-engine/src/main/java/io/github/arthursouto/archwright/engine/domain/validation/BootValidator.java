package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class BootValidator {

    private static final String BOOT_VERSION = "bootVersion";

    private final Catalog catalog;

    BootValidator(Catalog catalog) {
        this.catalog = catalog;
    }

    Checked<ResolvedBoot> validate(ProjectDraft draft) {
        Optional<ResolvedBoot> resolved = BootVersion.parse(draft.bootVersion())
                .filter(version -> version.compareTo(BootVersion.MINIMUM_SUPPORTED) >= 0)
                .flatMap(this::resolve);

        if (resolved.isEmpty()) {
            return Checked.invalid(Violation.of(
                    ViolationCode.UNSUPPORTED_BOOT_VERSION, BOOT_VERSION, "value", draft.bootVersion()));
        }

        return Checked.valid(resolved.get(), legacyWarnings(resolved.get().profile()));
    }

    private Optional<ResolvedBoot> resolve(BootVersion version) {
        return catalog.profileFor(version).map(profile -> new ResolvedBoot(version, profile));
    }

    private List<Warning> legacyWarnings(BootProfile profile) {
        if (!profile.legacy()) {
            return List.of();
        }

        return List.of(new Warning(WarningCode.LEGACY_BOOT_LINE, Map.of("line", profile.line())));
    }
}
