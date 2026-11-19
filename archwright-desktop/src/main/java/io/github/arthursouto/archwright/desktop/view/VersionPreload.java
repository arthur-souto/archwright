package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.VersionDefaults;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;

final class VersionPreload {

    private VersionPreload() {
    }

    static void start(CatalogQuery catalog, ProjectForm form) {
        Background.run(catalog::bootVersions, versions -> fill(catalog, form, versions), failure -> {
        });
    }

    private static void fill(CatalogQuery catalog, ProjectForm form, BootVersions versions) {
        if (!form.bootVersion().get().isBlank()) {
            return;
        }

        VersionDefaults.boot(versions.versions(), "").ifPresent(boot -> choose(catalog, form, boot));
    }

    private static void choose(CatalogQuery catalog, ProjectForm form, BootVersionOption boot) {
        form.bootVersion().set(boot.version());
        VersionDefaults.java(catalog.javaVersions(boot.version()), form.javaVersion().get())
                .ifPresent(java -> form.javaVersion().set(java.version()));
    }
}
