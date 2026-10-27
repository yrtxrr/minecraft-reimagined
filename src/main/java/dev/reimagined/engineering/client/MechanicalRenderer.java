package dev.reimagined.engineering.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.engineering.MachineBlockEntity;
import dev.reimagined.engineering.MechanicalBlock;
import java.util.Arrays;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** No world queries or client ticks: transform cached geometry using the synchronized motion sample. */
public final class MechanicalRenderer implements BlockEntityRenderer<MachineBlockEntity> {
    private static final float[] BRIGHTNESS = {1, 1, 1, 1};
    private final int[] light = new int[4];
    public MechanicalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(MachineBlockEntity machine, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int packedLight, int overlay) {
        var state = machine.getBlockState();
        var meshes = MechanicalModels.meshes();
        boolean mill = state.is(EngineeringContent.MILLSTONE.get());
        var quads = mill ? meshes.mill() : state.is(EngineeringContent.WATER_WHEEL.get()) ? meshes.wheel() : meshes.shaft();
        if (quads.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (mill) {
            pose.mulPose(Axis.YP.rotationDegrees((float) machine.rotationAngle(partialTick)));
        } else {
            Direction.Axis axis = state.getValue(MechanicalBlock.AXIS);
            if (axis == Direction.Axis.X) pose.mulPose(Axis.YP.rotationDegrees(90));
            else if (axis == Direction.Axis.Y) pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees((float) machine.rotationAngle(partialTick)));
        }
        pose.translate(-0.5, -0.5, -0.5);
        Arrays.fill(light, packedLight);
        var consumer = buffers.getBuffer(Sheets.solidBlockSheet());
        var transformed = pose.last();
        for (var quad : quads)
            consumer.putBulkData(transformed, quad, BRIGHTNESS, 1, 1, 1, 1, light, overlay, false);
        pose.popPose();
    }
}
