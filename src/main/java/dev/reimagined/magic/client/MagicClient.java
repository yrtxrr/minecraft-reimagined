package dev.reimagined.magic.client;

import dev.reimagined.magic.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="reimagined",value=Dist.CLIENT)
public final class MagicClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(MagicContent.RESEARCH_MENU.get(),MagicStationScreen::new);
        event.register(MagicContent.ALTAR_MENU.get(),MagicStationScreen::new);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {MagicProgressPayload.clear();}
}
