package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import javafx.application.HostServices;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class NativeDialogs implements Dialogs {

    private final Messages messages;
    private final HostServices hostServices;

    public NativeDialogs(Messages messages, HostServices hostServices) {
        this.messages = messages;
        this.hostServices = hostServices;
    }

    @Override
    public boolean confirmOverwrite(Window owner, String message) {
        ButtonType replace = new ButtonType(messages.text("export.overwrite.confirm"), ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, replace, ButtonType.CANCEL);
        alert.setTitle(messages.text("export.overwrite.title"));
        alert.setHeaderText(null);
        alert.initOwner(owner);

        return alert.showAndWait().filter(replace::equals).isPresent();
    }

    @Override
    public boolean confirmAction(Window owner, String message, String action) {
        ButtonType confirm = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, ButtonType.CANCEL);
        alert.setTitle(messages.text("app.title"));
        alert.setHeaderText(null);
        alert.initOwner(owner);

        return alert.showAndWait().filter(confirm::equals).isPresent();
    }

    @Override
    public Optional<Path> chooseDirectory(Window owner, Path initial) {
        DirectoryChooser chooser = new DirectoryChooser();

        if (Files.isDirectory(initial)) {
            chooser.setInitialDirectory(initial.toFile());
        }

        return Optional.ofNullable(chooser.showDialog(owner)).map(File::toPath);
    }

    @Override
    public void openFolder(Path folder) {
        hostServices.showDocument(folder.toUri().toString());
    }

    @Override
    public void openUrl(String url) {
        hostServices.showDocument(url);
    }
}
