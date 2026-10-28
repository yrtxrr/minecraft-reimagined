package dev.reimagined.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;

/** Single synchronous ray scope; never retain across ticks or chunk lifecycle changes.
 * Loaded-only view: rays collide with unavailable terrain rather than requesting a chunk future. */
public final class SpellWorldAccess implements BlockGetter {
    private final ServerLevel level;
    private long cachedChunkKey = Long.MIN_VALUE;
    private net.minecraft.world.level.chunk.LevelChunk cachedChunk;
    public SpellWorldAccess(ServerLevel level) {this.level=level;}
    private net.minecraft.world.level.chunk.LevelChunk chunkAt(BlockPos pos) {
        long key = net.minecraft.world.level.ChunkPos.asLong(pos);
        if (key == cachedChunkKey) return cachedChunk;
        cachedChunkKey = key;
        return cachedChunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }
    @Override public BlockState getBlockState(BlockPos pos) {
        var chunk=chunkAt(pos);
        return chunk==null?Blocks.BARRIER.defaultBlockState():chunk.getBlockState(pos);
    }
    @Override public FluidState getFluidState(BlockPos pos) {return getBlockState(pos).getFluidState();}
    @Override public BlockEntity getBlockEntity(BlockPos pos) {
        var chunk=chunkAt(pos);
        return chunk==null?null:chunk.getBlockEntity(pos);
    }
    @Override public int getHeight() {return level.getHeight();}
    @Override public int getMinBuildHeight() {return level.getMinBuildHeight();}
    public static BlockHitResult ray(ServerPlayer player,int range,ClipContext.Block mode) {
        if(range<1 || range>16)throw new IllegalArgumentException("Spell ray range exceeds budget");
        var start=player.getEyePosition();
        return new SpellWorldAccess(player.serverLevel()).clip(new ClipContext(start,start.add(player.getLookAngle().scale(range)),mode,ClipContext.Fluid.NONE,player));
    }
    public static java.util.List<net.minecraft.world.entity.LivingEntity> candidates(ServerPlayer player,Vec3 limit) {
        var result=new java.util.ArrayList<net.minecraft.world.entity.LivingEntity>(64);
        var delta=limit.subtract(player.getEyePosition());
        if(delta.length()>16.001)throw new IllegalArgumentException("Target range exceeds spell budget");
        player.serverLevel().getEntities().get(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.LivingEntity.class),
                player.getBoundingBox().expandTowards(delta).inflate(1),entity-> {
                    result.add(entity);
                    return result.size()>=64?net.minecraft.util.AbortableIterationConsumer.Continuation.ABORT:net.minecraft.util.AbortableIterationConsumer.Continuation.CONTINUE;
                });
        return result;
    }
    public static boolean loadedBox(ServerLevel level,AABB box) {
        int x0=net.minecraft.util.Mth.floor(box.minX)>>4,x1=net.minecraft.util.Mth.floor(box.maxX)>>4;
        int z0=net.minecraft.util.Mth.floor(box.minZ)>>4,z1=net.minecraft.util.Mth.floor(box.maxZ)>>4;
        if(x1-x0>1 || z1-z0>1)return false;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
}
