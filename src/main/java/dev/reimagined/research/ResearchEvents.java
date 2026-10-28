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

/** Local periodic/book-use observation:64creature candidates,8visibility rays and one6block sight ray. */
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
        if (player.isSpectator() || player.serverLevel().getChunkSource().getChunkNow(player.blockPosition().getX()>>4,player.blockPosition().getZ()>>4)==null) return;
        Discoveries before = Discoveries.read(player);
        var level=player.serverLevel();
        Discoveries after=before;
        // BiomeManager's Voronoi lookup can touch adjacent quart cells. Guard all nearby FULL chunks.
        if (dev.reimagined.magic.SpellWorldAccess.loadedBox(level,new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(2)))
            after=level.getBiome(player.blockPosition()).unwrapKey().map(key->before.withBiome(key.location())).orElse(before);
        if (after.size()<Discoveries.LIMIT) {
            final Discoveries[] found={after};final int[] examined={0},traces={0};
            level.getEntities().get(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),player.getBoundingBox().inflate(6),entity->{
                examined[0]++;
                if (!(entity instanceof Player) && entity.isAlive() && !entity.isSpectator()
                        && !found[0].creatures().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
                        && player.getEyePosition().distanceToSqr(entity.getEyePosition())<=256) {
                    traces[0]++;
                    var hit=new dev.reimagined.magic.SpellWorldAccess(level).clip(new net.minecraft.world.level.ClipContext(
                            player.getEyePosition(),entity.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                    if (hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS) found[0]=found[0].withCreature(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                }
                return examined[0]>=64 || traces[0]>=8 || found[0].size()>=Discoveries.LIMIT
                        ?net.minecraft.util.AbortableIterationConsumer.Continuation.ABORT:net.minecraft.util.AbortableIterationConsumer.Continuation.CONTINUE;
            });
            after=found[0];
        }
        if (after.size()<Discoveries.LIMIT) {
            // One six-block loaded-only sight ray, not a volume scan or a hidden-ore detector.
            var hit=dev.reimagined.magic.SpellWorldAccess.ray(player,6,net.minecraft.world.level.ClipContext.Block.OUTLINE);
            if (hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var pos=hit.getBlockPos();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                if (chunk!=null && RARE_BLOCKS.contains(chunk.getBlockState(pos).getBlock()))
                    after=after.withBlock(BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()));
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
