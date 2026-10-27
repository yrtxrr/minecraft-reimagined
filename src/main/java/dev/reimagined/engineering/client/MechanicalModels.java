package dev.reimagined.engineering.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Three immutable rotor meshes, rebuilt only by model baking/resource reload. */
@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class MechanicalModels {
    public static final ModelResourceLocation WHEEL = model("water_wheel_rotor");
    public static final ModelResourceLocation SHAFT = model("shaft_rotor");
    public static final ModelResourceLocation MILL = model("millstone_rotor");
    private static volatile Meshes meshes = new Meshes(List.of(), List.of(), List.of());
    private MechanicalModels() {}
    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("reimagined", "block/mechanical/" + name));
    }
    @SubscribeEvent public static void register(ModelEvent.RegisterAdditional event) {
        event.register(WHEEL); event.register(SHAFT); event.register(MILL);
    }
    @SubscribeEvent public static void bake(ModelEvent.BakingCompleted event) {
        // Atomic replacement means reload releases all old atlas/model references together.
        meshes = new Meshes(quads(event.getModels().get(WHEEL)), quads(event.getModels().get(SHAFT)), quads(event.getModels().get(MILL)));
    }
    private static List<BakedQuad> quads(BakedModel model) {
        if (model == null) throw new IllegalStateException("Mechanical rotor model was not baked");
        var result = new ArrayList<BakedQuad>();
        var random = RandomSource.create(42);
        for (Direction direction : Direction.values()) {
            random.setSeed(42);
            result.addAll(model.getQuads(null, direction, random, ModelData.EMPTY, null));
        }
        random.setSeed(42);
        result.addAll(model.getQuads(null, null, random, ModelData.EMPTY, null));
        return List.copyOf(result);
    }
    public static Meshes meshes() { return meshes; }
    public record Meshes(List<BakedQuad> wheel, List<BakedQuad> shaft, List<BakedQuad> mill) {}
}
