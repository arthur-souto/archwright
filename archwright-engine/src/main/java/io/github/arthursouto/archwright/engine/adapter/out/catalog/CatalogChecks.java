package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CatalogChecks {

    private CatalogChecks() {
    }

    static IllegalStateException problem(String file, String message) {
        return new IllegalStateException("catalog: " + file + ": " + message);
    }

    static void requireUnique(String file, String what, List<String> keys) {
        Set<String> seen = new HashSet<>();

        for (String key : keys) {
            if (!seen.add(key)) {
                throw problem(file, "duplicated " + what + " " + key);
            }
        }
    }
}
