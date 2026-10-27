package dev.reimagined.creature.client;

import dev.reimagined.creature.Deer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class DeerRenderer extends MobRenderer<Deer, DeerModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("reimagined", "textures/entity/deer.png");
    public DeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeerModel(context.bakeLayer(DeerModel.LAYER)), 0.5f);
    }
    @Override public ResourceLocation getTextureLocation(Deer deer) { return TEXTURE; }
}
