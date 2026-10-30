package dev.reimagined.core;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Lazy adapter construction: absent/disabled mods must never have their classes resolved. */
public final class IntegrationGate {
    private IntegrationGate() { }
    public static boolean run(String id, Set<? extends String> disabled, BooleanSupplier present, Supplier<? extends Runnable> adapter) {
        if (disabled.contains(id) || !present.getAsBoolean()) return false;
        adapter.get().run(); return true;
    }
}
