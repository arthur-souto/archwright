package io.github.arthursouto.archwright.engine.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDraftTest {

    @Test
    void nullTextsBecomeEmpty() {
        ProjectDraft draft = new ProjectDraft(
                null, null, null, null, null, null, null, null, null, null, null, true);

        assertThat(draft.artifactId()).isEmpty();
        assertThat(draft.groupId()).isEmpty();
        assertThat(draft.basePackage()).isEmpty();
        assertThat(draft.description()).isEmpty();
        assertThat(draft.bootVersion()).isEmpty();
    }

    @Test
    void nullListsBecomeEmpty() {
        ProjectDraft draft = new ProjectDraft(
                null, null, null, null, null, null, null, null, null, null, null, true);

        assertThat(draft.modules()).isEmpty();
        assertThat(draft.dependencies()).isEmpty();
        assertThat(draft.extras()).isEmpty();
    }

    @Test
    void nullListElementsBecomeEmptyTexts() {
        ProjectDraft draft = new ProjectDraft(
                null, null, null, null, null, Arrays.asList("pedidos", null), null, null, null, null, null, true);

        assertThat(draft.modules()).containsExactly("pedidos", "");
    }
}
