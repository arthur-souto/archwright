package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;

record ResolvedBoot(BootVersion version, BootProfile profile) {
}
