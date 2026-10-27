package dev.reimagined;

import dev.reimagined.content.Content;
import dev.reimagined.creature.CreatureContent;
import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.progression.ProgressionEvents;
import dev.reimagined.season.Seasons;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Single mod entry point. Modules own their registrations; gameplay stays server-authoritative. */
@Mod(Reimagined.MOD_ID)
public final class Reimagined {
    public static final String MOD_ID = "reimagined";
    public Reimagined(IEventBus modBus) {
        Content.register(modBus);
        CreatureContent.register(modBus);
        EngineeringContent.register(modBus);
        dev.reimagined.agriculture.AgricultureContent.register(modBus);
        ProgressionEvents.register();
        Seasons.register(modBus);
    }
}
