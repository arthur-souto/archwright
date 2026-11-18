package io.github.arthursouto.archwright.engine.domain.model;

public record ProjectIdentity(
        ArtifactId artifactId,
        GroupId groupId,
        BasePackage basePackage,
        Description description) {
}
