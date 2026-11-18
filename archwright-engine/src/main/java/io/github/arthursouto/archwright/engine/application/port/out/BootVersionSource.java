package io.github.arthursouto.archwright.engine.application.port.out;

import java.io.IOException;
import java.util.List;

public interface BootVersionSource {

    List<String> fetch() throws IOException;
}
