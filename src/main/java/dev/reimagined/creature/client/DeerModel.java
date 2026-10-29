package dev.reimagined.creature.client;

import dev.reimagined.creature.Deer;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Original deer mesh with independently mapped antlers and an anchored neck. */
public final class DeerModel extends QuadrupedModel<Deer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("reimagined", "deer"), "main");

    private final List<ModelPart> headAndNeck;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;

    public DeerModel(ModelPart root) {
        super(root, false, 8, 4, 2, 2, 24);
        // Apply the same baby transform to head and neck, but rotate only the head.
        // Rotating the entire neck around its top exposes a gap above the shoulders.
        headAndNeck = List.of(head, root.getChild("neck"));
        leftAntler = head.getChild("left_antler");
        rightAntler = head.getChild("right_antler");
    }

    @Override
    protected Iterable<ModelPart> headParts() { return headAndNeck; }

    @Override
    public void setupAnim(Deer deer, float swing, float amount, float age, float yaw, float pitch) {
        super.setupAnim(deer, swing, amount, age, yaw, pitch);
        leftAntler.visible = !deer.isBaby();
        rightAntler.visible = !deer.isBaby();
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5f, -5, -5, 5, 5, 6), PartPose.offset(0, 4, -5));
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(22, 0).addBox(-1.5f, -1, -2, 3, 5, 3), PartPose.offset(0, 4, -5));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 12)
                .addBox(0, -3, -1, 4, 1, 2), PartPose.offsetAndRotation(1.5f, -4, 0, 0, 0, -0.35f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-4, -3, -1, 4, 1, 2), PartPose.offsetAndRotation(-1.5f, -4, 0, 0, 0, 0.35f));
        // Dedicated atlas region x50..61/y16..23, clear of torso, head and legs.
        head.addOrReplaceChild("left_antler", CubeListBuilder.create().texOffs(50, 16)
                .addBox(1, -10, 0, 1, 7, 1).addBox(1, -9, -2, 3, 1, 3)
                .addBox(3, -12, -2, 1, 4, 1), PartPose.offset(0, -1, 0));
        head.addOrReplaceChild("right_antler", CubeListBuilder.create().texOffs(50, 16)
                .addBox(-2, -10, 0, 1, 7, 1).addBox(-4, -9, -2, 3, 1, 3)
                .addBox(-4, -12, -2, 1, 4, 1), PartPose.offset(0, -1, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-3, -4, -7, 6, 8, 14), PartPose.offset(0, 10, 1));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 12)
                .addBox(-1, -1, 0, 2, 3, 1), PartPose.offset(0, -2, 7));
        root.addOrReplaceChild("right_hind_leg", leg(), PartPose.offset(-2, 13, 6));
        root.addOrReplaceChild("left_hind_leg", leg(), PartPose.offset(2, 13, 6));
        root.addOrReplaceChild("right_front_leg", leg(), PartPose.offset(-2, 13, -4));
        root.addOrReplaceChild("left_front_leg", leg(), PartPose.offset(2, 13, -4));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder leg() {
        return CubeListBuilder.create().texOffs(42, 0).addBox(-1, 0, -1, 2, 11, 2);
    }
}
