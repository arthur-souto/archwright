package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;

import java.util.function.Consumer;

interface Navigation {

    void show(Section section);

    void startProject(Consumer<ProjectForm> prefill);
}
