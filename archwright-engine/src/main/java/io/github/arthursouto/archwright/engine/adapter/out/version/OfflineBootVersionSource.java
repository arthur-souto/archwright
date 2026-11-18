package io.github.arthursouto.archwright.engine.adapter.out.version;

import io.github.arthursouto.archwright.engine.application.port.out.BootVersionSource;

import java.io.IOException;
import java.util.List;

public final class OfflineBootVersionSource implements BootVersionSource {

    @Override
    public List<String> fetch() throws IOException {
        throw new IOException("offline mode: remote boot versions are disabled");
    }
}
