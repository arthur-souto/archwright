package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.FitnessRuleSummary;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewFile;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PreviewProject implements PreviewProjectQuery {

    private static final String DESCRIPTION = "description";
    private static final byte LINE_FEED = '\n';

    private final ProjectPlanning planning;
    private final ProfileResolver resolver;
    private final ProjectRenderer renderer;

    public PreviewProject(ProjectPlanning planning, ProfileResolver resolver, ProjectRenderer renderer) {
        this.planning = planning;
        this.resolver = resolver;
        this.renderer = renderer;
    }

    @Override
    public PreviewResult preview(ProjectRequest request) {
        return switch (planning.prepare(request)) {
            case ProjectPlanning.Outcome.Rejected rejected -> new PreviewResult.Failure(rejected.violations());
            case ProjectPlanning.Outcome.Ready ready -> preview(ready);
        };
    }

    private PreviewResult preview(ProjectPlanning.Outcome.Ready ready) {
        List<PreviewFile> files = files(ready);

        return new PreviewResult.Preview(
                files.stream().map(PreviewFile::path).toList(),
                files,
                dependencies(ready.spec()),
                fitnessRules(ready),
                ready.warnings());
    }

    private List<PreviewFile> files(ProjectPlanning.Outcome.Ready ready) {
        Map<String, String> descriptions = ready.plan().files().stream()
                .filter(planned -> planned.variables().containsKey(DESCRIPTION))
                .collect(Collectors.toMap(PlannedFile::target, planned -> planned.variables().get(DESCRIPTION)));

        return renderer.render(ready.plan(), ready.context()).files().stream()
                .map(file -> new PreviewFile(file.path(), lines(file), descriptions.get(file.path())))
                .sorted(Comparator.comparing(PreviewFile::path))
                .toList();
    }

    private int lines(GeneratedFile file) {
        byte[] content = file.content();
        int lines = 0;

        for (byte value : content) {
            if (value == LINE_FEED) {
                lines++;
            }
        }

        return content.length > 0 && content[content.length - 1] != LINE_FEED ? lines + 1 : lines;
    }

    private List<FitnessRuleSummary> fitnessRules(ProjectPlanning.Outcome.Ready ready) {
        return ready.context().sections("fitness.rules").stream()
                .map(rule -> new FitnessRuleSummary(rule.text("id"), rule.text("testClass"), rule.text("description")))
                .toList();
    }

    private List<ResolvedDependency> dependencies(ProjectSpec spec) {
        BootProfile profile = resolver.profileFor(spec.bootVersion());

        return spec.dependencies().stream()
                .flatMap(dependency -> DependencyArtifacts.of(dependency, profile).stream())
                .toList();
    }
}
