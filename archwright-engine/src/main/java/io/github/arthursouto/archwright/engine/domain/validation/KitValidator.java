package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.KitId;

final class KitValidator {

    private static final String KIT = "kit";

    private final Kits kits;

    KitValidator(Kits kits) {
        this.kits = kits;
    }

    Checked<KitId> validate(ProjectDraft draft) {
        return kits.find(draft.kit())
                .map(strategy -> Checked.valid(strategy.id()))
                .orElseGet(() -> Checked.invalid(Violation.of(ViolationCode.UNKNOWN_KIT, KIT, "value", draft.kit())));
    }
}
