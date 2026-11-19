package io.github.arthursouto.archwright.desktop.wizard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WizardStepTest {

    @Test
    void movesForwardAndBackwardWithinTheBounds() {
        assertThat(WizardStep.PROJECT.next()).isEqualTo(WizardStep.VERSIONS);
        assertThat(WizardStep.EXPORT.previous()).isEqualTo(WizardStep.DEPENDENCIES);
        assertThat(WizardStep.PROJECT.previous()).isEqualTo(WizardStep.PROJECT);
        assertThat(WizardStep.EXPORT.next()).isEqualTo(WizardStep.EXPORT);
    }

    @Test
    void knowsFirstAndLast() {
        assertThat(WizardStep.PROJECT.isFirst()).isTrue();
        assertThat(WizardStep.EXPORT.isLast()).isTrue();
        assertThat(WizardStep.VERSIONS.isFirst()).isFalse();
        assertThat(WizardStep.VERSIONS.isLast()).isFalse();
    }

    @Test
    void mapsEveryEngineFieldToExactlyOneStep() {
        for (String field : new String[] {
                "artifactId", "groupId", "basePackage", "description", "architectureStyle", "modules",
                "bootVersion", "javaVersion", "kit", "dependencies", "extras", "destination"}) {
            assertThat(WizardStep.owning(field)).as(field).isPresent();
            assertThat(java.util.Arrays.stream(WizardStep.values()).filter(step -> step.owns(field))).as(field).hasSize(1);
        }
    }

    @Test
    void ignoresUnknownFields() {
        assertThat(WizardStep.owning("nothing")).isEmpty();
    }
}
