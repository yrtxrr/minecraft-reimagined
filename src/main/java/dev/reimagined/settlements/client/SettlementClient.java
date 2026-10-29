package dev.reimagined.settlements.client;
import dev.reimagined.settlements.SettlementContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
/** MerchantScreen is registered by Minecraft; only the settler renderer needs a mod hook. */
@EventBusSubscriber(modid="reimagined",value=Dist.CLIENT)
public final class SettlementClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SettlementContent.SETTLER.get(),SettlerRenderer::new);
    }
    private SettlementClient() {}
}
