package io.github.arthursouto.archwright.engine.application.port.in;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportTargetTest {

    @Test
    void keepsFormatDestinationAndOverwriteFlag() {
        ExportTarget target = new ExportTarget(ExportFormat.ZIP, Path.of("/tmp/x"), true);

        assertThat(target.format()).isEqualTo(ExportFormat.ZIP);
        assertThat(target.destination()).isEqualTo(Path.of("/tmp/x"));
        assertThat(target.overwrite()).isTrue();
    }

    @Test
    void requiresFormatAndDestination() {
        assertThatThrownBy(() -> new ExportTarget(null, Path.of("/tmp/x"), false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExportTarget(ExportFormat.FOLDER, null, false)).isInstanceOf(NullPointerException.class);
    }
}
