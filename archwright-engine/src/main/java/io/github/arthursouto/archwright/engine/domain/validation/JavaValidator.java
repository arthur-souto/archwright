package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.JavaVersion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JavaValidator {

    private static final String JAVA_VERSION = "javaVersion";

    Checked<JavaVersion> validate(ProjectDraft draft, Checked<ResolvedBoot> boot) {
        Optional<JavaVersion> parsed = JavaVersion.parse(draft.javaVersion());

        if (parsed.isEmpty()) {
            return Checked.invalid(Violation.of(
                    ViolationCode.UNSUPPORTED_JAVA_VERSION, JAVA_VERSION, "value", draft.javaVersion()));
        }

        if (!boot.isValid()) {
            return Checked.valid(parsed.get());
        }

        return checkAgainstProfile(parsed.get(), boot.value());
    }

    private Checked<JavaVersion> checkAgainstProfile(JavaVersion java, ResolvedBoot boot) {
        BootProfile profile = boot.profile();

        if (!profile.supports(java)) {
            return Checked.invalid(incompatible(profile, boot));
        }

        if (!profile.isTested(java)) {
            return Checked.valid(java, List.of(aboveTested(java, profile)));
        }

        return Checked.valid(java);
    }

    private Violation incompatible(BootProfile profile, ResolvedBoot boot) {
        return new Violation(
                ViolationCode.INCOMPATIBLE_JAVA_VERSION,
                JAVA_VERSION,
                Map.of("min", String.valueOf(profile.javaMin()), "boot", boot.version().toString()));
    }

    private Warning aboveTested(JavaVersion java, BootProfile profile) {
        return new Warning(
                WarningCode.JAVA_ABOVE_TESTED,
                Map.of("java", java.toString(), "maxTested", String.valueOf(profile.javaMaxTested())));
    }
}
