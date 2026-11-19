package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.DependencyOption;
import io.github.arthursouto.archwright.engine.application.port.in.ExtraOption;
import io.github.arthursouto.archwright.engine.application.port.in.FitnessRuleSummary;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import javafx.collections.FXCollections;
import javafx.collections.SetChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class DependenciesStepView implements StepView {

    private static final double SPACING = 16;
    private static final double GROUP_SPACING = 6;
    private static final double GROUP_GAP = 40;
    private static final double GROUP_MIN_WIDTH = 220;

    private final CatalogQuery catalog;
    private final Messages messages;
    private final ProjectForm form;
    private final LiveValidation validation;
    private final ComboBox<KitOption> kits = new ComboBox<>();
    private final FlowPane dependencyGroups = new FlowPane(GROUP_GAP, SPACING);
    private final VBox extrasBox = new VBox(GROUP_SPACING);
    private final VBox rulesBox = new VBox(GROUP_SPACING);
    private final CheckBox fitness = new CheckBox();
    private final Map<String, CheckBox> dependencyBoxes = new LinkedHashMap<>();
    private final Map<String, CheckBox> extraBoxes = new LinkedHashMap<>();
    private final VBox root = new VBox(SPACING);

    DependenciesStepView(DesktopContext context, ProjectForm form, LiveValidation validation) {
        this.catalog = context.catalog();
        this.messages = context.messages();
        this.form = form;
        this.validation = validation;

        configureKits();
        configureFitness();
        buildExtras();
        root.getChildren().addAll(
                title(),
                new FieldRow(messages.text("dependencies.kit"), kits, WizardStep.DEPENDENCIES, "kit", validation, form.kit()),
                dependencyGroups,
                heading("dependencies.extras"),
                extrasBox,
                fitness,
                rulesBox);
        keepViewsInSyncWithForm();
    }

    @Override
    public Node content() {
        return root;
    }

    @Override
    public void onShow() {
        rebuildDependencies();
        refreshExtras();
        refreshRules();
    }

    private Label title() {
        Label title = new Label(messages.text("dependencies.title"));
        title.getStyleClass().add(Styles.TITLE_3);

        return title;
    }

    private Label heading(String key) {
        Label heading = new Label(messages.text(key));
        heading.getStyleClass().add(Styles.TEXT_BOLD);

        return heading;
    }

    private void configureKits() {
        List<KitOption> options = catalog.kits();
        kits.setItems(FXCollections.observableList(options));
        kits.setMaxWidth(Double.MAX_VALUE);
        kits.setCellFactory(list -> Cells.of(KitOption::label));
        kits.setButtonCell(Cells.of(KitOption::label));
        kits.getSelectionModel().select(initialKit(options));
        applyKit(kits.getValue());
        kits.valueProperty().addListener((observable, before, after) -> applyKit(after));
    }

    private KitOption initialKit(List<KitOption> options) {
        return options.stream()
                .filter(option -> option.id().equals(form.kit().get()))
                .findFirst()
                .orElse(options.getFirst());
    }

    private void applyKit(KitOption kit) {
        if (kit == null) {
            return;
        }

        form.kit().set(kit.id());
        form.dependencies().clear();
        form.dependencies().addAll(kit.dependencies());
    }

    private void configureFitness() {
        fitness.setText(messages.text("dependencies.fitness"));
        fitness.selectedProperty().bindBidirectional(form.fitnessFunctions());
        fitness.selectedProperty().addListener(observable -> refreshRules());
    }

    private void buildExtras() {
        for (ExtraOption extra : catalog.extras()) {
            CheckBox box = new CheckBox(extra.label());
            box.selectedProperty().addListener((observable, before, after) -> toggle(form.extras(), extra.id(), after));
            extraBoxes.put(extra.id(), box);
            extrasBox.getChildren().add(box);
        }
    }

    private void rebuildDependencies() {
        List<DependencyOption> available = catalog.dependencies(form.bootVersion().get());
        Set<String> availableIds = available.stream().map(DependencyOption::id).collect(Collectors.toSet());
        form.dependencies().removeIf(id -> !availableIds.contains(id));

        dependencyBoxes.clear();
        dependencyGroups.getChildren().setAll(groups(available));
    }

    private List<Node> groups(List<DependencyOption> available) {
        Map<String, List<DependencyOption>> byGroup = available.stream()
                .collect(Collectors.groupingBy(this::groupName, LinkedHashMap::new, Collectors.toList()));

        return byGroup.entrySet().stream().map(entry -> group(entry.getKey(), entry.getValue())).toList();
    }

    private String groupName(DependencyOption option) {
        return option.group() == null || option.group().isBlank() ? messages.text("dependencies.group.other") : option.group();
    }

    private Node group(String name, List<DependencyOption> options) {
        VBox box = new VBox(GROUP_SPACING);
        box.setMinWidth(GROUP_MIN_WIDTH);
        Label label = new Label(name);
        label.getStyleClass().add(Styles.TEXT_BOLD);
        box.getChildren().add(label);
        options.forEach(option -> box.getChildren().add(dependencyBox(option)));

        return box;
    }

    private Node dependencyBox(DependencyOption option) {
        CheckBox box = new CheckBox(option.label());
        box.setSelected(form.dependencies().contains(option.id()));
        box.selectedProperty().addListener((observable, before, after) -> toggle(form.dependencies(), option.id(), after));
        box.setTooltip(new Tooltip(option.description()));
        dependencyBoxes.put(option.id(), box);

        if (option.implies().isEmpty()) {
            return box;
        }

        Label implied = new Label(messages.text("dependencies.implies", "value", String.join(", ", option.implies())));
        implied.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        return new VBox(2, box, implied);
    }

    private void toggle(Set<String> values, String id, boolean selected) {
        if (selected) {
            values.add(id);
        } else {
            values.remove(id);
        }
    }

    private void keepViewsInSyncWithForm() {
        form.dependencies().addListener((SetChangeListener<String>) change -> {
            dependencyBoxes.forEach((id, box) -> box.setSelected(form.dependencies().contains(id)));
            refreshExtras();
            refreshRules();
        });
        form.extras().addListener((SetChangeListener<String>) change ->
                extraBoxes.forEach((id, box) -> box.setSelected(form.extras().contains(id))));
        form.architectureStyle().addListener(observable -> refreshRules());
    }

    private void refreshExtras() {
        for (ExtraOption extra : catalog.extras()) {
            boolean satisfied = form.dependencies().containsAll(extra.requires());
            CheckBox box = extraBoxes.get(extra.id());
            box.setDisable(!satisfied);

            if (!satisfied) {
                box.setSelected(false);
            }
        }
    }

    private void refreshRules() {
        rulesBox.getChildren().clear();
        rulesBox.setVisible(form.fitnessFunctions().get());
        rulesBox.setManaged(rulesBox.isVisible());

        if (!form.fitnessFunctions().get()) {
            return;
        }

        rulesBox.getChildren().add(heading("dependencies.fitness.rules"));
        rules().forEach(rule -> rulesBox.getChildren().add(ruleLabel(rule)));
    }

    private List<FitnessRuleSummary> rules() {
        return catalog.fitnessRules(
                form.architectureStyle().get(), form.selectedDependencies(), form.bootVersion().get());
    }

    private Label ruleLabel(FitnessRuleSummary rule) {
        Label label = new Label(rule.id() + " — " + rule.description());
        label.setWrapText(true);
        label.getStyleClass().add(Styles.TEXT_SMALL);

        return label;
    }
}
