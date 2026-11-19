package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import io.github.arthursouto.archwright.engine.application.port.in.DependencyOption;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PresetTest {

    @TempDir
    Path workspace;

    @Test
    void offersThreeStartingPoints() {
        assertThat(Preset.all()).extracting(Preset::id).containsExactly("modular-basic", "layered-basic", "api-minimal");
    }

    @Test
    void fillsTheFormWithTheStyleKitDependenciesAndFitnessFunctions() {
        ProjectForm form = new ProjectForm();
        form.extras().add("github-actions");
        form.fitnessFunctions().set(false);

        Preset.all().getLast().applyTo(form);

        assertThat(form.architectureStyle().get()).isEqualTo("HEXAGONAL_LAYERED");
        assertThat(form.kit().get()).isEqualTo("NONE");
        assertThat(form.selectedDependencies()).containsExactly("actuator", "validation", "web");
        assertThat(form.extras()).isEmpty();
        assertThat(form.fitnessFunctions().get()).isTrue();
        assertThat(form.dependenciesChosen()).isTrue();
    }

    @Test
    void theBasicKitPresetsCarryTheSameDependenciesAsTheCatalogKit() {
        ArchwrightEngine engine = ArchwrightEngine.builder().cacheDirectory(workspace).offline(true).build();
        KitOption basic = engine.catalog().kits().stream().filter(kit -> kit.id().equals("BASIC")).findFirst().orElseThrow();

        assertThat(Preset.all().getFirst().dependencies()).containsExactlyElementsOf(basic.dependencies());
        assertThat(Preset.all().get(1).dependencies()).containsExactlyElementsOf(basic.dependencies());
    }

    @Test
    void everyPresetPointsAtThingsTheEngineKnows() {
        ArchwrightEngine engine = ArchwrightEngine.builder().cacheDirectory(workspace).offline(true).build();
        List<String> styles = engine.catalog().architectureStyles().stream().map(ArchitectureStyleOption::id).toList();
        List<String> kits = engine.catalog().kits().stream().map(KitOption::id).toList();
        List<String> dependencies = engine.catalog().dependencies("4.1.2").stream().map(DependencyOption::id).toList();

        for (Preset preset : Preset.all()) {
            assertThat(styles).as(preset.id()).contains(preset.architectureStyle());
            assertThat(kits).as(preset.id()).contains(preset.kit());
            assertThat(dependencies).as(preset.id()).containsAll(preset.dependencies());
        }
    }

    @Test
    void everyPresetHasATitleAndADescription() {
        Messages messages = Messages.portuguese();

        for (Preset preset : Preset.all()) {
            assertThat(messages.has("preset." + preset.id() + ".title")).as(preset.id()).isTrue();
            assertThat(messages.has("preset." + preset.id() + ".description")).as(preset.id()).isTrue();
        }
    }
}
