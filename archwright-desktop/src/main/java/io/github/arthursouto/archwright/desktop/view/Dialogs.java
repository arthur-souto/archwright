package io.github.arthursouto.archwright.desktop.view;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Optional;

public interface Dialogs {

    boolean confirmOverwrite(Window owner, String message);

    Optional<Path> chooseDirectory(Window owner, Path initial);

    void openFolder(Path folder);
}
