package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.ArtifactId;
import io.github.arthursouto.archwright.engine.domain.model.BasePackage;
import io.github.arthursouto.archwright.engine.domain.model.Description;
import io.github.arthursouto.archwright.engine.domain.model.GroupId;
import io.github.arthursouto.archwright.engine.domain.model.ProjectIdentity;
import io.github.arthursouto.archwright.engine.domain.naming.Names;
import io.github.arthursouto.archwright.engine.domain.naming.PackageSegments;

import java.util.Optional;

final class IdentityValidator {

    private static final String ARTIFACT_ID = "artifactId";
    private static final String GROUP_ID = "groupId";
    private static final String BASE_PACKAGE = "basePackage";
    private static final int MIN_PACKAGE_SEGMENTS = 2;
    private static final String DESCRIPTION = "description";

    Checked<ProjectIdentity> validate(ProjectDraft draft) {
        Checked<ArtifactId> artifactId = checkArtifactId(draft.artifactId());
        Checked<GroupId> groupId = checkGroupId(draft.groupId());
        Checked<BasePackage> basePackage = checkBasePackage(draft.basePackage(), artifactId, groupId);
        Checked<Description> description = checkDescription(draft.description());

        if (!Checked.allValid(artifactId, groupId, basePackage, description)) {
            return Checked.invalid(Checked.violationsOf(artifactId, groupId, basePackage, description));
        }

        return Checked.valid(new ProjectIdentity(
                artifactId.value(), groupId.value(), basePackage.value(), description.value()));
    }

    private Checked<ArtifactId> checkArtifactId(String raw) {
        return ArtifactId.parse(raw)
                .map(Checked::valid)
                .orElseGet(() -> Checked.invalid(Violation.of(ViolationCode.INVALID_ARTIFACT_ID, ARTIFACT_ID)));
    }

    private Checked<GroupId> checkGroupId(String raw) {
        return GroupId.parse(raw)
                .map(Checked::valid)
                .orElseGet(() -> Checked.invalid(Violation.of(ViolationCode.INVALID_GROUP_ID, GROUP_ID)));
    }

    private Checked<BasePackage> checkBasePackage(
            String raw, Checked<ArtifactId> artifactId, Checked<GroupId> groupId) {
        Optional<String> candidate = basePackageCandidate(raw.trim(), artifactId, groupId);

        if (candidate.isEmpty()) {
            return Checked.skipped();
        }

        return validatePackage(candidate.get());
    }

    private Optional<String> basePackageCandidate(
            String typed, Checked<ArtifactId> artifactId, Checked<GroupId> groupId) {
        if (!typed.isEmpty()) {
            return Optional.of(typed);
        }

        if (!artifactId.isValid() || !groupId.isValid()) {
            return Optional.empty();
        }

        return Optional.of(Names.suggestBasePackage(groupId.value().value(), artifactId.value().value()));
    }

    private Checked<BasePackage> validatePackage(String candidate) {
        Optional<String> invalidSegment = PackageSegments.firstInvalid(candidate);

        if (invalidSegment.isPresent()) {
            return Checked.invalid(
                    Violation.of(ViolationCode.INVALID_BASE_PACKAGE, BASE_PACKAGE, "segment", invalidSegment.get()));
        }

        if (PackageSegments.split(candidate).size() < MIN_PACKAGE_SEGMENTS) {
            return Checked.invalid(Violation.of(ViolationCode.BASE_PACKAGE_TOO_SHORT, BASE_PACKAGE));
        }

        return Checked.valid(new BasePackage(candidate));
    }

    private Checked<Description> checkDescription(String raw) {
        return Description.parse(raw)
                .map(Checked::valid)
                .orElseGet(() -> Checked.invalid(Violation.of(
                        ViolationCode.INVALID_DESCRIPTION, DESCRIPTION, "max", String.valueOf(Description.MAX_LENGTH))));
    }
}
