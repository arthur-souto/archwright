package io.github.arthursouto.archwright.engine.application.port.in;

public record ResolvedDependency(String id, String groupId, String artifactId, String version, String scope) {
}
