package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ProvisionalRequest {

    static final String ARTIFACT_ID = "projeto";
    static final String GROUP_ID = "com.empresa";
    static final String MODULE = "modulo";

    private ProvisionalRequest() {
    }

    public static Optional<ProjectRequest> repair(ProjectRequest request, List<Violation> violations) {
        ProjectRequest repaired = new ProjectRequest(
                fixed(request.artifactId(), "artifactId", ARTIFACT_ID, violations),
                fixed(request.groupId(), "groupId", GROUP_ID, violations),
                fixed(request.basePackage(), "basePackage", "", violations),
                request.description(),
                request.architectureStyle(),
                modules(request.modules(), violations),
                request.bootVersion(),
                request.javaVersion(),
                request.kit(),
                request.dependencies(),
                request.extras(),
                request.fitnessFunctions());

        return repaired.equals(request) ? Optional.empty() : Optional.of(repaired);
    }

    private static String fixed(String current, String field, String replacement, List<Violation> violations) {
        return violations.stream().anyMatch(violation -> violation.field().equals(field)) ? replacement : current;
    }

    private static List<String> modules(List<String> current, List<Violation> violations) {
        List<Violation> about = violations.stream().filter(violation -> violation.field().equals("modules")).toList();

        if (about.isEmpty()) {
            return current;
        }

        Set<String> rejected = new LinkedHashSet<>();
        about.forEach(violation -> {
            if (violation.params().containsKey("name")) {
                rejected.add(violation.params().get("name"));
            }
        });

        List<String> kept = new ArrayList<>(new LinkedHashSet<>(current));
        kept.removeAll(rejected);

        return kept.isEmpty() ? List.of(MODULE) : limited(kept, about);
    }

    private static List<String> limited(List<String> kept, List<Violation> about) {
        return about.stream()
                .filter(violation -> violation.code().equals("TOO_MANY_MODULES"))
                .findFirst()
                .map(violation -> kept.subList(0, Math.min(kept.size(), Integer.parseInt(violation.params().get("max")))))
                .orElse(kept);
    }
}
