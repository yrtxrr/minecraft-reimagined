package dev.reimagined.smithing.client;

import dev.reimagined.smithing.SmithingContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid="reimagined",value=Dist.CLIENT)
public final class SmithingClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(SmithingContent.MENU.get(),ForgeScreen::new);}
}
