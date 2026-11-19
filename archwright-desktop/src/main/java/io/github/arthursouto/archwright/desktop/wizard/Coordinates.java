package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;

public final class Coordinates {

    private static final String DEFAULT_SCOPE = "compile";

    private Coordinates() {
    }

    public static String artifact(ResolvedDependency dependency) {
        return dependency.artifactId() + versionOf(dependency) + scopeOf(dependency);
    }

    public static String format(ResolvedDependency dependency) {
        return dependency.groupId() + ":" + artifact(dependency);
    }

    private static String versionOf(ResolvedDependency dependency) {
        return dependency.version() == null || dependency.version().isBlank() ? "" : ":" + dependency.version();
    }

    private static String scopeOf(ResolvedDependency dependency) {
        return DEFAULT_SCOPE.equals(dependency.scope()) ? "" : " (" + dependency.scope() + ")";
    }
}
