package dev.reimagined.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;

/** Loaded-only view: rays collide with unavailable terrain rather than requesting a chunk future. */
public final class SpellWorldAccess implements BlockGetter {
    private final ServerLevel level;
    public SpellWorldAccess(ServerLevel level) {this.level=level;}
    @Override public BlockState getBlockState(BlockPos pos) {
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk==null?Blocks.BARRIER.defaultBlockState():chunk.getBlockState(pos);
    }
    @Override public FluidState getFluidState(BlockPos pos) {return getBlockState(pos).getFluidState();}
    @Override public BlockEntity getBlockEntity(BlockPos pos) {return null;}
    @Override public int getHeight() {return level.getHeight();}
    @Override public int getMinBuildHeight() {return level.getMinBuildHeight();}
    public static BlockHitResult ray(ServerPlayer player,int range,ClipContext.Block mode) {
        if(range<1 || range>16)throw new IllegalArgumentException("Spell ray range exceeds budget");
        var start=player.getEyePosition();
        return new SpellWorldAccess(player.serverLevel()).clip(new ClipContext(start,start.add(player.getLookAngle().scale(range)),mode,ClipContext.Fluid.NONE,player));
    }
    public static boolean loadedBox(ServerLevel level,AABB box) {
        int x0=net.minecraft.util.Mth.floor(box.minX)>>4,x1=net.minecraft.util.Mth.floor(box.maxX)>>4;
        int z0=net.minecraft.util.Mth.floor(box.minZ)>>4,z1=net.minecraft.util.Mth.floor(box.maxZ)>>4;
        if(x1-x0>1 || z1-z0>1)return false;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
}
