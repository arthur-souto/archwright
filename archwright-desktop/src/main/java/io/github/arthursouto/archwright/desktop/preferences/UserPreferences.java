package io.github.arthursouto.archwright.desktop.preferences;

import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;

import java.util.prefs.Preferences;

public final class UserPreferences {

    private static final String GROUP_ID = "groupId";
    private static final String ARCHITECTURE_STYLE = "architectureStyle";
    private static final String KIT = "kit";
    private static final String FITNESS_FUNCTIONS = "fitnessFunctions";
    private static final String FORMAT = "format";
    private static final String DESTINATION = "destination";
    private static final String DARK_THEME = "darkTheme";

    private final Preferences node;

    public UserPreferences(Preferences node) {
        this.node = node;
    }

    public static UserPreferences system() {
        return new UserPreferences(Preferences.userNodeForPackage(UserPreferences.class));
    }

    public void restoreInto(ProjectForm form) {
        form.groupId().set(node.get(GROUP_ID, ""));
        form.architectureStyle().set(node.get(ARCHITECTURE_STYLE, ""));
        form.kit().set(node.get(KIT, ""));
        form.fitnessFunctions().set(node.getBoolean(FITNESS_FUNCTIONS, true));
        form.format().set(storedFormat());
        form.destination().set(node.get(DESTINATION, ""));
    }

    public void remember(ProjectForm form) {
        node.put(GROUP_ID, form.groupId().get().strip());
        node.put(ARCHITECTURE_STYLE, form.architectureStyle().get());
        node.put(KIT, form.kit().get());
        node.putBoolean(FITNESS_FUNCTIONS, form.fitnessFunctions().get());
        node.put(FORMAT, form.format().get().name());
        node.put(DESTINATION, form.destination().get().strip());
    }

    public boolean darkTheme() {
        return node.getBoolean(DARK_THEME, false);
    }

    public void darkTheme(boolean dark) {
        node.putBoolean(DARK_THEME, dark);
    }

    private ExportFormat storedFormat() {
        try {
            return ExportFormat.valueOf(node.get(FORMAT, ExportFormat.ZIP.name()));
        } catch (IllegalArgumentException unknown) {
            return ExportFormat.ZIP;
        }
    }
}
