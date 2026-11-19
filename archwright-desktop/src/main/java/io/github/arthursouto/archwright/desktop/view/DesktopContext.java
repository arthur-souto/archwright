package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.history.ProjectHistory;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.preferences.UserPreferences;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.GenerateProjectUseCase;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;

public record DesktopContext(
        CatalogQuery catalog,
        PreviewProjectQuery preview,
        GenerateProjectUseCase generator,
        Messages messages,
        UserPreferences preferences,
        ProjectHistory history,
        Dialogs dialogs) {
}
