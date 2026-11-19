package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VersionDefaultsTest {

    private final List<BootVersionOption> boots = List.of(
            new BootVersionOption("4.1.2", "4.1", false, true),
            new BootVersionOption("4.1.1", "4.1", false, false),
            new BootVersionOption("2.7.18", "2.7", true, true));

    private final List<JavaVersionOption> javas = List.of(
            new JavaVersionOption("17", true, false, false),
            new JavaVersionOption("21", true, false, true),
            new JavaVersionOption("25", true, false, false));

    @Test
    void keepsTheCurrentBootWhenItIsStillOffered() {
        assertThat(VersionDefaults.boot(boots, "4.1.1")).map(BootVersionOption::version).contains("4.1.1");
    }

    @Test
    void otherwisePicksTheLatestSupportedBoot() {
        assertThat(VersionDefaults.boot(boots, "")).map(BootVersionOption::version).contains("4.1.2");
        assertThat(VersionDefaults.boot(boots, "9.9.9")).map(BootVersionOption::version).contains("4.1.2");
    }

    @Test
    void fallsBackToTheFirstBootWhenAllAreLegacy() {
        List<BootVersionOption> legacy = List.of(new BootVersionOption("2.7.18", "2.7", true, true));

        assertThat(VersionDefaults.boot(legacy, "")).map(BootVersionOption::version).contains("2.7.18");
    }

    @Test
    void noBootOptionsGiveNothing() {
        assertThat(VersionDefaults.boot(List.of(), "")).isEmpty();
    }

    @Test
    void keepsTheCurrentJavaOrUsesTheSuggestedOne() {
        assertThat(VersionDefaults.java(javas, "25")).map(JavaVersionOption::version).contains("25");
        assertThat(VersionDefaults.java(javas, "8")).map(JavaVersionOption::version).contains("21");
    }

    @Test
    void fallsBackToTheFirstJavaWhenNoneIsSuggested() {
        List<JavaVersionOption> plain = List.of(new JavaVersionOption("17", true, false, false));

        assertThat(VersionDefaults.java(plain, "")).map(JavaVersionOption::version).contains("17");
    }
}
