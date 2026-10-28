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
        for (String species : new String[]{"otter","badger","marmot"})
            event.registerLayerDefinition(SmallFaunaModel.layerId(species), () -> SmallFaunaModel.layer(species));
    }
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CreatureContent.DEER.get(), DeerRenderer::new);
        event.registerEntityRenderer(CreatureContent.BOAR.get(), BoarRenderer::new);
        event.registerEntityRenderer(CreatureContent.OTTER.get(), c -> new SmallFaunaRenderer<>(c,"otter"));
        event.registerEntityRenderer(CreatureContent.BADGER.get(), c -> new SmallFaunaRenderer<>(c,"badger"));
        event.registerEntityRenderer(CreatureContent.MARMOT.get(), c -> new SmallFaunaRenderer<>(c,"marmot"));
    }
    private CreatureClient() {}
}
