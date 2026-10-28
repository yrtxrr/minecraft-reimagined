package dev.reimagined.research;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Registry IDs only: no entity references or chunk tickets are retained in player saves. */
public record Discoveries(List<ResourceLocation> biomes, List<ResourceLocation> creatures, List<ResourceLocation> blocks) {
    public static final String DATA_KEY = "reimagined_discoveries_v1";
    public static final int LIMIT = 256;
    public Discoveries { 
        biomes = List.copyOf(biomes); 
        creatures = List.copyOf(creatures); 
        blocks = List.copyOf(blocks != null ? blocks : List.of());
    }
    public int size() { return biomes.size() + creatures.size() + blocks.size(); }
    public static Discoveries read(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(DATA_KEY);
        if (tag.getInt("Version") != 1) return new Discoveries(List.of(), List.of(), List.of());
        var registry = player.registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceLocation> biomes = ids(tag.getList("Biomes", Tag.TAG_STRING), registry::containsKey, LIMIT);
        List<ResourceLocation> creatures = ids(tag.getList("Creatures", Tag.TAG_STRING), BuiltInRegistries.ENTITY_TYPE::containsKey, LIMIT - biomes.size());
        List<ResourceLocation> blocks = ids(tag.getList("Blocks", Tag.TAG_STRING), BuiltInRegistries.BLOCK::containsKey, Math.max(0, LIMIT - biomes.size() - creatures.size()));
        return new Discoveries(biomes, creatures, blocks);
    }
    private static List<ResourceLocation> ids(ListTag input, Predicate<ResourceLocation> known, int limit) {
        if (limit <= 0) return List.of();
        TreeSet<ResourceLocation> ids = new TreeSet<>();
        // The saved list is bounded even if another tool edited NBT; do not scan arbitrary huge lists.
        for (int i = 0; i < Math.min(input.size(), LIMIT) && ids.size() < limit; i++) {
            ResourceLocation id = ResourceLocation.tryParse(input.getString(i));
            if (id != null && known.test(id)) ids.add(id);
        }
        return List.copyOf(ids);
    }
    public Discoveries withBiome(ResourceLocation id) { return added(id, true); }
    public Discoveries withCreature(ResourceLocation id) { return added(id, false); }
    public Discoveries withBlock(ResourceLocation id) { return added(id, null); }
    private Discoveries added(ResourceLocation id, Boolean biome) {
        if (id == null || size() >= LIMIT) return this;
        if (Boolean.TRUE.equals(biome)) {
            if (biomes.contains(id)) return this;
            ArrayList<ResourceLocation> result = new ArrayList<>(biomes);
            result.add(id); result.sort(ResourceLocation::compareTo);
            return new Discoveries(result, creatures, blocks);
        } else if (Boolean.FALSE.equals(biome)) {
            if (creatures.contains(id)) return this;
            ArrayList<ResourceLocation> result = new ArrayList<>(creatures);
            result.add(id); result.sort(ResourceLocation::compareTo);
            return new Discoveries(biomes, result, blocks);
        } else {
            if (blocks.contains(id)) return this;
            ArrayList<ResourceLocation> result = new ArrayList<>(blocks);
            result.add(id); result.sort(ResourceLocation::compareTo);
            return new Discoveries(biomes, creatures, result);
        }
    }
    public void save(Player player) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        tag.put("Biomes", strings(biomes));
        tag.put("Creatures", strings(creatures));
        tag.put("Blocks", strings(blocks));
        if(tag.equals(player.getPersistentData().getCompound(DATA_KEY)))return;
        player.getPersistentData().put(DATA_KEY, tag);
        if (player instanceof net.minecraft.server.level.ServerPlayer server) dev.reimagined.magic.MagicProgressPayload.sync(server);
    }
    private static ListTag strings(List<ResourceLocation> values) {
        ListTag result = new ListTag();
        for (ResourceLocation id : values) result.add(StringTag.valueOf(id.toString()));
        return result;
    }
}
