package dev.reimagined.engineering;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Recomputed only near active machinery; never reads an unloaded chunk. */
public final class MechanicalNetwork {
    private MechanicalNetwork() {}
    public static Rotation inspect(Level level, BlockPos start) {
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(start.immutable());
        int wheels = 0, machines = 0;
        while (!queue.isEmpty() && visited.size() < Rotation.MAX_NODES) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos) || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof MechanicalBlock)) continue;
            if (state.is(EngineeringContent.WATER_WHEEL.get()) && powered(level, pos, state)) wheels++;
            if (state.is(EngineeringContent.MILLSTONE.get()) && !level.hasNeighborSignal(pos)) machines++;
            for (Direction direction : Direction.values()) {
                if (!connects(state, direction)) continue;
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || !level.hasChunkAt(next)) continue;
                BlockState neighbor = level.getBlockState(next);
                if (neighbor.getBlock() instanceof MechanicalBlock && connects(neighbor, direction.getOpposite()))
                    queue.addLast(next.immutable());
            }
        }
        return new Rotation(wheels, machines, !queue.isEmpty());
    }
    private static boolean connects(BlockState state, Direction face) {
        return state.is(EngineeringContent.MILLSTONE.get()) || state.getValue(MechanicalBlock.AXIS) == face.getAxis();
    }
    public static boolean powered(Level level, BlockPos pos, BlockState state) {
        if (level.hasNeighborSignal(pos)) return false;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == state.getValue(MechanicalBlock.AXIS)) continue;
            BlockPos water = pos.relative(direction);
            if (!level.hasChunkAt(water)) continue;
            var fluid = level.getFluidState(water);
            if (fluid.is(FluidTags.WATER) && fluid.getFlow(level, water).lengthSqr() > 0.001) return true;
        }
        return false;
    }
}
