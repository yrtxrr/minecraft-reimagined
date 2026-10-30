package dev.reimagined.civilization;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Registers the bounded settlement simulator and its operator commands. No new registries exist yet. */
public final class CivilizationContent {
    private CivilizationContent() {}

    public static void register() {
        CivilizationSimulation.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> CivilizationCommands.register(event));
    }
}
