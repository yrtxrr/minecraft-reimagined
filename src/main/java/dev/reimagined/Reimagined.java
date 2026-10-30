package dev.reimagined;

import dev.reimagined.content.Content;
import dev.reimagined.creature.CreatureContent;
import dev.reimagined.fishing.FishContent;
import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.progression.ProgressionEvents;
import dev.reimagined.season.Seasons;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Single mod entry point. Modules own their registrations; gameplay stays server-authoritative. */
@Mod(Reimagined.MOD_ID)
public final class Reimagined {
    public static final String MOD_ID = "reimagined";
    public Reimagined(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        dev.reimagined.core.CoreBootstrap.register(container);
        Content.register(modBus);
        dev.reimagined.content.ReimaginedTabs.register(modBus);
        CreatureContent.register(modBus);
        FishContent.register(modBus);
        EngineeringContent.register(modBus);
        dev.reimagined.agriculture.AgricultureContent.register(modBus);
        dev.reimagined.agriculture.OrchardContent.register(modBus);
        dev.reimagined.cooking.CookingContent.register(modBus);
        dev.reimagined.beekeeping.BeekeepingContent.register(modBus);
        dev.reimagined.magic.MagicContent.register(modBus);
        dev.reimagined.smithing.SmithingContent.register(modBus);
        dev.reimagined.settlements.SettlementContent.register(modBus);
        ProgressionEvents.register();
        Seasons.register(modBus);
    }
}
