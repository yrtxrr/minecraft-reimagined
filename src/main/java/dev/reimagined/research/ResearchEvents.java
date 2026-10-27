package dev.reimagined.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** A local observation every ten seconds, with at most eight visibility traces; no remote simulation. */
public final class ResearchEvents {
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
        if (!after.equals(before)) after.save(player);
    }
    private static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(Discoveries.DATA_KEY))
            event.getEntity().getPersistentData().put(Discoveries.DATA_KEY,
                    event.getOriginal().getPersistentData().getCompound(Discoveries.DATA_KEY).copy());
    }
}
