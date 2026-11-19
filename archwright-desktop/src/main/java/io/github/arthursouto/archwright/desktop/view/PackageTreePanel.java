package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.PackageNode;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PackageTreePanel extends VBox {

    private static final double SPACING = 8;
    private static final double WIDTH = 340;
    private static final double STALE_OPACITY = 0.5;
    private static final double DETAIL_SPACING = 4;

    private final Messages messages;
    private final LivePackageTree live;
    private final TreeView<PackageNode> tree = new TreeView<>(new TreeItem<>());
    private final Label note = new Label();
    private final Label totals = new Label();
    private final Label detailName = new Label();
    private final Label detailDescription = new Label();
    private final Label detailNumbers = new Label();
    private final VBox detail = new VBox(DETAIL_SPACING, detailName, detailDescription, detailNumbers);

    PackageTreePanel(Messages messages, LivePackageTree live) {
        super(SPACING);
        this.messages = messages;
        this.live = live;

        Label title = new Label(messages.text("tree.title"));
        title.getStyleClass().add(Styles.TITLE_4);

        configureTree();
        configureNote();
        configureDetail();
        totals.getStyleClass().add(Styles.TEXT_SMALL);
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);
        setPadding(new Insets(0, 24, 24, 0));
        VBox.setVgrow(tree, Priority.ALWAYS);
        getChildren().addAll(title, Hint.of(messages.text("tree.help")), note, totals, tree, detail);

        live.snapshot().addListener(observable -> render());
        live.stale().addListener(observable -> render());
        render();
    }

    private void configureTree() {
        tree.setShowRoot(false);
        tree.setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(PackageNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
                setStyle(item != null && item.file() ? "-fx-text-fill: -color-fg-muted;" : "");
            }
        });
        tree.opacityProperty().bind(Bindings.when(live.stale()).then(STALE_OPACITY).otherwise(1.0));
        tree.getSelectionModel().selectedItemProperty().addListener((observable, before, after) -> showDetail(after));
    }

    private void configureNote() {
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: -color-warning-fg;");
        note.getStyleClass().add(Styles.TEXT_SMALL);
        note.managedProperty().bind(note.visibleProperty());
    }

    private void configureDetail() {
        detailName.setWrapText(true);
        detailName.getStyleClass().add(Styles.TEXT_BOLD);
        detailDescription.setWrapText(true);
        detailNumbers.setWrapText(true);
        detailNumbers.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        detailDescription.managedProperty().bind(detailDescription.textProperty().isNotEmpty());
        detail.setPadding(new Insets(SPACING));
        detail.getStyleClass().add(Styles.BG_SUBTLE);
    }

    private void render() {
        LivePackageTree.Snapshot snapshot = live.snapshot().get();
        String selected = selectedName();

        tree.getRoot().getChildren().setAll(snapshot == null ? List.of() : items(snapshot.roots()));
        select(selected);
        renderTotals(snapshot);

        String text = noteText(snapshot);
        note.setText(text);
        note.setVisible(!text.isEmpty());
    }

    private void renderTotals(LivePackageTree.Snapshot snapshot) {
        boolean known = snapshot != null;
        totals.setText(known ? messages.text("tree.totals", Map.of(
                "files", String.valueOf(snapshot.files()), "lines", String.valueOf(snapshot.lines()))) : "");
        totals.setVisible(known);
        totals.setManaged(known);
    }

    private String selectedName() {
        TreeItem<PackageNode> item = tree.getSelectionModel().getSelectedItem();

        return item == null ? "" : item.getValue().qualifiedName();
    }

    private void select(String qualifiedName) {
        Optional<TreeItem<PackageNode>> same = find(tree.getRoot(), qualifiedName);
        Optional<TreeItem<PackageNode>> target = same.or(() -> tree.getRoot().getChildren().stream().findFirst());

        target.ifPresentOrElse(item -> tree.getSelectionModel().select(item), () -> showDetail(null));
    }

    private Optional<TreeItem<PackageNode>> find(TreeItem<PackageNode> from, String qualifiedName) {
        if (qualifiedName.isEmpty()) {
            return Optional.empty();
        }

        if (from.getValue() != null && from.getValue().qualifiedName().equals(qualifiedName)) {
            return Optional.of(from);
        }

        return from.getChildren().stream().map(child -> find(child, qualifiedName)).flatMap(Optional::stream).findFirst();
    }

    private void showDetail(TreeItem<PackageNode> item) {
        if (item == null || item.getValue() == null) {
            detailName.setText(messages.text("tree.detail.none"));
            detailDescription.setText("");
            detailNumbers.setText("");
            return;
        }

        PackageNode node = item.getValue();
        detailName.setText(node.file() ? node.name() : node.qualifiedName());
        detailDescription.setText(node.description());
        detailNumbers.setText(node.file()
                ? messages.text("tree.detail.file", "lines", String.valueOf(node.lines()))
                : messages.text("tree.detail.package", Map.of(
                        "packages", String.valueOf(node.packages()),
                        "classes", String.valueOf(node.classes()),
                        "lines", String.valueOf(node.totalLines()))));
    }

    private String noteText(LivePackageTree.Snapshot snapshot) {
        if (live.stale().get()) {
            return messages.text("tree.stale");
        }

        if (snapshot == null) {
            return messages.text("tree.empty");
        }

        return snapshot.provisional() ? messages.text("tree.provisional") : "";
    }

    private List<TreeItem<PackageNode>> items(List<PackageNode> nodes) {
        return nodes.stream().map(this::item).toList();
    }

    private TreeItem<PackageNode> item(PackageNode node) {
        TreeItem<PackageNode> item = new TreeItem<>(node);
        item.setExpanded(true);
        item.getChildren().setAll(items(node.children()));

        return item;
    }
}
