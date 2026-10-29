package dev.reimagined.settlements.client;

import dev.reimagined.settlements.SettlementContent;
import dev.reimagined.settlements.TradingMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class SettlementClient {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(SettlementContent.TRADING_MENU.get(), TradingScreen::new);
    }

    private SettlementClient() {}
}