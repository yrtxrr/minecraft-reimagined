package dev.reimagined.engineering.client;

import dev.reimagined.engineering.EngineeringContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class EngineeringClient {
    private EngineeringClient() {}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EngineeringContent.MACHINE.get(), MechanicalRenderer::new);
    }
}
