package dev.reimagined.research;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

/** A local observation every ten seconds, with at most eight visibility traces; no remote simulation. */
public final class ResearchEvents {
    private static final Set<Block> RARE_BLOCKS = Set.of(
            Blocks.AMETHYST_CLUSTER, Blocks.BUDDING_AMETHYST, Blocks.CALCITE, Blocks.TUFF,
            Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_ORE,
            Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD
    );
    private ResearchEvents() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ResearchEvents::tick);
        NeoForge.EVENT_BUS.addListener(ResearchEvents::clonePlayer);
    }
    private static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 200 == 0) observe(player);
    }
    public static void observe(ServerPlayer player) {
        if (player.isSpectator() || !player.serverLevel().hasChunkAt(player.blockPosition())) return;
        Discoveries before = Discoveries.read(player);
        Discoveries after = player.serverLevel().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> before.withBiome(key.location())).orElse(before);
        if (after.size() < Discoveries.LIMIT) {
            final Discoveries known = after;
            final int[] traces = {0};
            var visible = player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6),
                    entity -> !(entity instanceof Player) && entity.isAlive() && !entity.isSpectator()
                            && !known.creatures().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
                            && traces[0]++ < 8 && player.hasLineOfSight(entity));
            for (LivingEntity entity : visible) after = after.withCreature(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        }
        if (after.size() < Discoveries.LIMIT) {
            int range = 4;
            for (int dx = -range; dx <= range && after.size() < Discoveries.LIMIT; dx++) {
                for (int dy = -range; dy <= range && after.size() < Discoveries.LIMIT; dy++) {
                    for (int dz = -range; dz <= range && after.size() < Discoveries.LIMIT; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 6) continue;
                        BlockPos pos = player.blockPosition().offset(dx, dy, dz);
                        if (!player.serverLevel().hasChunkAt(pos)) continue;
                        BlockState state = player.serverLevel().getBlockState(pos);
                        Block block = state.getBlock();
                        if (RARE_BLOCKS.contains(block)) {
                            after = after.withBlock(BuiltInRegistries.BLOCK.getKey(block));
                        }
                    }
                }
            }
        }
        if (!after.equals(before)) after.save(player);
    }
    private static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(Discoveries.DATA_KEY))
            event.getEntity().getPersistentData().put(Discoveries.DATA_KEY,
                    event.getOriginal().getPersistentData().getCompound(Discoveries.DATA_KEY).copy());
    }
}
