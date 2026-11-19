package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import io.github.arthursouto.archwright.engine.application.port.in.Warning;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import io.github.arthursouto.archwright.engine.domain.validation.ProjectDraft;
import io.github.arthursouto.archwright.engine.domain.validation.SpecValidator;
import io.github.arthursouto.archwright.engine.domain.validation.ValidationResult;

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

    Outcome prepare(ProjectRequest request) {
        return switch (validator.validate(draft(request))) {
            case ValidationResult.Invalid invalid -> new Outcome.Rejected(PortViews.violations(invalid.violations()));
            case ValidationResult.Valid valid -> ready(valid);
        };
    }

    private Outcome ready(ValidationResult.Valid valid) {
        TemplateContext context = contextBuilder.build(valid.spec());

        return new Outcome.Ready(valid.spec(), PortViews.warnings(valid.warnings()), context, planner.plan(valid.spec(), context));
    }

    private ProjectDraft draft(ProjectRequest request) {
        return new ProjectDraft(
                request.artifactId(),
                request.groupId(),
                request.basePackage(),
                request.description(),
                request.architectureStyle(),
                request.modules(),
                request.bootVersion(),
                request.javaVersion(),
                request.kit(),
                request.dependencies(),
                request.extras(),
                request.fitnessFunctions());
    }
}
