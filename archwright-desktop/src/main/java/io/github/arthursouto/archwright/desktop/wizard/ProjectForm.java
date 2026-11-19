package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.desktop.history.HistoryEntry;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ProjectForm {

    private final StringProperty artifactId = new SimpleStringProperty("");
    private final StringProperty groupId = new SimpleStringProperty("");
    private final StringProperty basePackage = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty architectureStyle = new SimpleStringProperty("");
    private final StringProperty modules = new SimpleStringProperty("");
    private final StringProperty bootVersion = new SimpleStringProperty("");
    private final StringProperty javaVersion = new SimpleStringProperty("");
    private final StringProperty kit = new SimpleStringProperty("");
    private final ObservableSet<String> dependencies = FXCollections.observableSet();
    private final ObservableSet<String> extras = FXCollections.observableSet();
    private final BooleanProperty fitnessFunctions = new SimpleBooleanProperty(true);
    private final ObjectProperty<ExportFormat> format = new SimpleObjectProperty<>(ExportFormat.ZIP);
    private final StringProperty destination = new SimpleStringProperty("");
    private boolean dependenciesChosen;

    public StringProperty artifactId() {
        return artifactId;
    }

    public StringProperty groupId() {
        return groupId;
    }

    public StringProperty basePackage() {
        return basePackage;
    }

    public StringProperty description() {
        return description;
    }

    public StringProperty architectureStyle() {
        return architectureStyle;
    }

    public StringProperty modules() {
        return modules;
    }

    public StringProperty bootVersion() {
        return bootVersion;
    }

    public StringProperty javaVersion() {
        return javaVersion;
    }

    public StringProperty kit() {
        return kit;
    }

    public ObservableSet<String> dependencies() {
        return dependencies;
    }

    public ObservableSet<String> extras() {
        return extras;
    }

    public BooleanProperty fitnessFunctions() {
        return fitnessFunctions;
    }

    public ObjectProperty<ExportFormat> format() {
        return format;
    }

    public StringProperty destination() {
        return destination;
    }

    public ProjectRequest toRequest() {
        return new ProjectRequest(
                artifactId.get().strip(),
                groupId.get().strip(),
                basePackage.get().strip(),
                description.get().strip(),
                architectureStyle.get(),
                moduleNames(),
                bootVersion.get(),
                javaVersion.get(),
                kit.get(),
                selectedDependencies(),
                sorted(extras),
                fitnessFunctions.get());
    }

    public List<String> selectedDependencies() {
        return sorted(dependencies);
    }

    public void onChange(Runnable listener) {
        List.of(artifactId, groupId, basePackage, description, architectureStyle, modules, bootVersion, javaVersion,
                kit, fitnessFunctions, format, destination)
                .forEach(property -> property.addListener(observable -> listener.run()));
        dependencies.addListener((SetChangeListener<String>) change -> listener.run());
        extras.addListener((SetChangeListener<String>) change -> listener.run());
    }

    public void useDependencies(Collection<String> chosen) {
        dependencies.clear();
        dependencies.addAll(chosen);
        dependenciesChosen = true;
    }

    public boolean dependenciesChosen() {
        return dependenciesChosen;
    }

    public void load(HistoryEntry entry) {
        artifactId.set(entry.artifactId());
        groupId.set(entry.groupId());
        basePackage.set(entry.basePackage());
        description.set(entry.description());
        architectureStyle.set(entry.architectureStyle());
        modules.set(String.join(", ", entry.modules()));
        bootVersion.set(entry.bootVersion());
        javaVersion.set(entry.javaVersion());
        kit.set(entry.kit());
        useDependencies(entry.dependencies());
        extras.clear();
        extras.addAll(entry.extras());
        fitnessFunctions.set(entry.fitnessFunctions());
        format.set(entry.format());
        destination.set(entry.destination());
    }

    public boolean hasDestination() {
        return !destination.get().isBlank();
    }

    public ExportTarget toTarget(boolean overwrite) {
        return new ExportTarget(format.get(), Path.of(destination.get().strip()), overwrite);
    }

    private List<String> moduleNames() {
        return Arrays.stream(modules.get().split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private List<String> sorted(ObservableSet<String> values) {
        return values.stream().sorted().toList();
    }
}
