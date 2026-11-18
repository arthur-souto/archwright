package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.Architecture;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;
import io.github.arthursouto.archwright.engine.domain.model.JavaVersion;
import io.github.arthursouto.archwright.engine.domain.model.KitId;
import io.github.arthursouto.archwright.engine.domain.model.ProjectIdentity;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;

import java.util.List;

public final class SpecValidator {

    private final IdentityValidator identityValidator;
    private final StyleValidator styleValidator;
    private final BootValidator bootValidator;
    private final JavaValidator javaValidator;
    private final KitValidator kitValidator;
    private final DependencyValidator dependencyValidator;
    private final ExtraValidator extraValidator;

    public SpecValidator(Catalog catalog, ArchitectureStyles styles, Kits kits) {
        this.identityValidator = new IdentityValidator();
        this.styleValidator = new StyleValidator(styles);
        this.bootValidator = new BootValidator(catalog);
        this.javaValidator = new JavaValidator();
        this.kitValidator = new KitValidator(kits);
        this.dependencyValidator = new DependencyValidator(catalog);
        this.extraValidator = new ExtraValidator(catalog);
    }

    public ValidationResult validate(ProjectDraft draft) {
        Checked<ProjectIdentity> identity = identityValidator.validate(draft);
        Checked<Architecture> architecture = styleValidator.validate(draft);
        Checked<ResolvedBoot> boot = bootValidator.validate(draft);
        Checked<JavaVersion> java = javaValidator.validate(draft, boot);
        Checked<KitId> kit = kitValidator.validate(draft);
        Checked<List<DependencyId>> dependencies = dependencyValidator.validate(draft, boot);
        Checked<List<ExtraId>> extras = extraValidator.validate(draft, dependencies);

        if (!Checked.allValid(identity, architecture, boot, java, kit, dependencies, extras)) {
            return new ValidationResult.Invalid(
                    Checked.violationsOf(identity, architecture, boot, java, kit, dependencies, extras));
        }

        ProjectSpec spec = new ProjectSpec(
                identity.value(),
                architecture.value(),
                boot.value().version(),
                java.value(),
                kit.value(),
                dependencies.value(),
                extras.value(),
                draft.fitnessFunctions());

        return new ValidationResult.Valid(spec, Checked.warningsOf(boot, java, dependencies));
    }
}
