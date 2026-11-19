package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.util.function.Consumer;

public final class AppShell extends BorderPane implements Navigation {

    private static final String DARK = "dark";

    private final DesktopContext context;
    private final Sidebar sidebar;
    private final Node about;
    private final Node docs;
    private WizardView wizard;
    private boolean wizardFinished;

    public AppShell(DesktopContext context) {
        this.context = context;
        this.about = new AboutView(context);
        this.docs = new DocsView(context);
        this.sidebar = new Sidebar(context.messages(), this::show, this::toggleTheme);

        getStylesheets().add(Brand.stylesheet());
        setLeft(sidebar);
        applyTheme(context.preferences().darkTheme());
        show(Section.HOME);
    }

    @Override
    public void show(Section section) {
        setCenter(switch (section) {
            case HOME -> new HomeView(context, this);
            case WIZARD -> wizardView();
            case DOCS -> docs;
            case HISTORY -> new HistoryView(context, this);
            case ABOUT -> about;
        });
        sidebar.select(section);
    }

    @Override
    public void startProject(Consumer<ProjectForm> prefill) {
        ProjectForm form = new ProjectForm();
        context.preferences().restoreInto(form);
        prefill.accept(form);

        wizardFinished = false;
        wizard = new WizardView(context, form, () -> wizardFinished = true);
        show(Section.WIZARD);
    }

    private Node wizardView() {
        if (wizard == null || wizardFinished) {
            startProject(form -> {
            });
        }

        return wizard;
    }

    private void toggleTheme() {
        boolean dark = !context.preferences().darkTheme();
        context.preferences().darkTheme(dark);
        applyTheme(dark);
    }

    private void applyTheme(boolean dark) {
        ThemeSwitcher.apply(dark);
        sidebar.themeIsDark(dark);
        getStyleClass().remove(DARK);

        if (dark) {
            getStyleClass().add(DARK);
        }
    }
}
