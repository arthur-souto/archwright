package io.github.arthursouto.archwright.engine.domain.kit;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Kits {

    private final Map<String, KitStrategy> strategies;

    public Kits(Collection<KitStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(strategy -> strategy.id().value(), Function.identity()));
    }

    public static Kits standard() {
        return new Kits(List.of(new NoneKit(), new BasicKit()));
    }

    public List<KitStrategy> all() {
        return strategies.values().stream()
                .sorted(Comparator.comparing(strategy -> strategy.id().value()))
                .toList();
    }

    public Optional<KitStrategy> find(String rawId) {
        return Optional.ofNullable(strategies.get(rawId.trim()));
    }
}
