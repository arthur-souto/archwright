package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import io.github.arthursouto.archwright.engine.domain.validation.ProjectDraft;
import io.github.arthursouto.archwright.engine.domain.validation.SpecValidator;
import io.github.arthursouto.archwright.engine.domain.validation.ValidationResult;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.Warning;

import java.util.List;

public final class ProjectPlanning {

    sealed interface Outcome {

        record Ready(ProjectSpec spec, List<Warning> warnings, TemplateContext context, FilePlan plan) implements Outcome {
        }

        record Rejected(List<Violation> violations) implements Outcome {
        }
    }

    private final SpecValidator validator;
    private final ContextBuilder contextBuilder;
    private final FilePlanner planner;

    public ProjectPlanning(SpecValidator validator, ContextBuilder contextBuilder, FilePlanner planner) {
        this.validator = validator;
        this.contextBuilder = contextBuilder;
        this.planner = planner;
    }

    Outcome prepare(ProjectDraft draft) {
        return switch (validator.validate(draft)) {
            case ValidationResult.Invalid invalid -> new Outcome.Rejected(invalid.violations());
            case ValidationResult.Valid valid -> ready(valid);
        };
    }

    private Outcome ready(ValidationResult.Valid valid) {
        TemplateContext context = contextBuilder.build(valid.spec());

        return new Outcome.Ready(valid.spec(), valid.warnings(), context, planner.plan(valid.spec(), context));
    }
}
