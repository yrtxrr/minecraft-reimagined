package dev.reimagined.cooking.client;

import dev.reimagined.cooking.CookingContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid="reimagined", value=Dist.CLIENT)
public final class CookingClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) { event.register(CookingContent.MENU.get(),CookingPotScreen::new); }
}
