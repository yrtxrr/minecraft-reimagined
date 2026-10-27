package dev.reimagined.engineering;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Main-thread, non-requesting access. hasChunkAt only checks tickets on ServerLevel, not FULL readiness. */
final class MechanicalWorldAccess {
    private MechanicalWorldAccess() {}
    static LevelChunk chunk(Level level, BlockPos pos) {
        int x = SectionPos.blockToSectionCoord(pos.getX());
        int z = SectionPos.blockToSectionCoord(pos.getZ());
        if (level instanceof ServerLevel server) return server.getChunkSource().getChunkNow(x, z);
        var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
        return chunk instanceof LevelChunk full ? full : null;
    }
    static boolean loaded(Level level, BlockPos pos) { return chunk(level, pos) != null; }
    static BlockEntity blockEntity(Level level, BlockPos pos) {
        var chunk = chunk(level, pos);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }
}
