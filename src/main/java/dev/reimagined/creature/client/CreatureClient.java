package dev.reimagined.creature.client;

import dev.reimagined.creature.CreatureContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class CreatureClient {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DeerModel.LAYER, DeerModel::layer);
        event.registerLayerDefinition(BoarModel.LAYER, BoarModel::layer);
    }
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CreatureContent.DEER.get(), DeerRenderer::new);
        event.registerEntityRenderer(CreatureContent.BOAR.get(), BoarRenderer::new);
    }
    private CreatureClient() {}
}
