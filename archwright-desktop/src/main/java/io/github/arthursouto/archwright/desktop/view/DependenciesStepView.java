package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.Coordinates;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.DependencyOption;
import io.github.arthursouto.archwright.engine.application.port.in.ExtraOption;
import io.github.arthursouto.archwright.engine.application.port.in.FitnessRuleSummary;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;
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
    private static final double HINT_GAP = 2;
    private static final double GROUP_GAP = 40;
    private static final double GROUP_WIDTH = 300;

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
    private final Label kitMarks = new Label();
    private final VBox root = new VBox(SPACING);
    private List<DependencyOption> available = List.of();

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
                new FieldRow(messages.text("dependencies.kit"), messages.text("dependencies.kit.help"), new VBox(GROUP_SPACING, kits, kitMarks), WizardStep.DEPENDENCIES, "kit", validation, form.kit()),
                dependencyGroups,
                new VBox(HINT_GAP, heading("dependencies.extras"), Hint.of(messages.text("dependencies.extras.help"))),
                extrasBox,
                new VBox(HINT_GAP, fitness, Hint.indented(messages.text("dependencies.fitness.help"))),
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
        kitMarks.setWrapText(true);
        kitMarks.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
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
        showKitMarks(kit);
    }

    private void showKitMarks(KitOption kit) {
        String labels = kit.dependencies().stream().map(this::labelOf).collect(Collectors.joining(", "));

        kitMarks.setText(kit.dependencies().isEmpty()
                ? messages.text("dependencies.kit.empty")
                : messages.text("dependencies.kit.marks", "value", labels));
    }

    private String labelOf(String id) {
        return available.stream().filter(option -> option.id().equals(id)).map(DependencyOption::label).findFirst().orElse(id);
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
            extrasBox.getChildren().add(extraRow(extra, box));
        }
    }

    private Node extraRow(ExtraOption extra, CheckBox box) {
        String key = "extra." + extra.id() + ".help";

        VBox row = new VBox(HINT_GAP, box);

        if (messages.has(key)) {
            row.getChildren().add(Hint.indented(messages.text(key)));
        }

        if (messages.has("extra." + extra.id() + ".files")) {
            row.getChildren().add(Hint.indented(messages.text("extra." + extra.id() + ".files")));
        }

        return row;
    }

    private void rebuildDependencies() {
        available = catalog.dependencies(form.bootVersion().get());
        Set<String> availableIds = available.stream().map(DependencyOption::id).collect(Collectors.toSet());
        form.dependencies().removeIf(id -> !availableIds.contains(id));

        dependencyBoxes.clear();
        dependencyGroups.getChildren().setAll(groups(available));
        showKitMarks(kits.getValue());
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
        box.setPrefWidth(GROUP_WIDTH);
        box.setMaxWidth(GROUP_WIDTH);
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
        dependencyBoxes.put(option.id(), box);

        VBox row = new VBox(HINT_GAP, box, Hint.indented(option.description()));
        option.artifacts().forEach(artifact -> row.getChildren().add(artifactHint(artifact)));

        if (!option.implies().isEmpty()) {
            row.getChildren().add(Hint.indented(messages.text("dependencies.implies", "value", String.join(", ", option.implies()))));
        }

        return row;
    }

    private Node artifactHint(ResolvedDependency artifact) {
        Label hint = Hint.indented(messages.text("dependencies.artifact", "value", Coordinates.artifact(artifact)));
        hint.setTooltip(new Tooltip(Coordinates.format(artifact)));

        return hint;
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
        rules().forEach(rule -> rulesBox.getChildren().add(ruleRow(rule)));
    }

    private List<FitnessRuleSummary> rules() {
        return catalog.fitnessRules(
                form.architectureStyle().get(), form.selectedDependencies(), form.bootVersion().get());
    }

    private Node ruleRow(FitnessRuleSummary rule) {
        Label label = new Label(rule.id() + " — " + rule.description());
        label.setWrapText(true);
        label.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_BOLD);
        VBox row = new VBox(HINT_GAP, label);
        String key = "rule." + rule.id() + ".help";

        if (messages.has(key)) {
            row.getChildren().add(Hint.of(messages.text(key)));
        }

        return row;
    }
}
