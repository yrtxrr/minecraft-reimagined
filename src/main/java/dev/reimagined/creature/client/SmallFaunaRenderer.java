package dev.reimagined.creature.client;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;

public final class SmallFaunaRenderer<T extends Animal> extends MobRenderer<T,SmallFaunaModel<T>> {
    private final ResourceLocation texture;
    public SmallFaunaRenderer(EntityRendererProvider.Context context,String species) {
        super(context,new SmallFaunaModel<>(context.bakeLayer(SmallFaunaModel.layerId(species))),0.35f);
        texture=ResourceLocation.fromNamespaceAndPath("reimagined","textures/entity/"+species+".png");
    }
    @Override public ResourceLocation getTextureLocation(T mob) { return texture; }
}
