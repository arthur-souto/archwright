package io.github.arthursouto.archwright.engine.application.port.in;


import java.util.List;

public sealed interface PreviewResult {

    record Preview(
            List<String> paths,
            List<PreviewFile> files,
            List<ResolvedDependency> dependencies,
            List<FitnessRuleSummary> fitnessRules,
            List<Warning> warnings) implements PreviewResult {

        public Preview {
            paths = List.copyOf(paths);
            files = List.copyOf(files);
            dependencies = List.copyOf(dependencies);
            fitnessRules = List.copyOf(fitnessRules);
            warnings = List.copyOf(warnings);
        }
    }

    record Failure(List<Violation> violations) implements PreviewResult {

        public Failure {
            violations = List.copyOf(violations);
        }
    }
}
