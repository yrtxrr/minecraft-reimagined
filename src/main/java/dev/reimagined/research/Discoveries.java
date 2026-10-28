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
public record Discoveries(List<ResourceLocation> biomes, List<ResourceLocation> creatures) {
    public static final String DATA_KEY = "reimagined_discoveries_v1";
    public static final int LIMIT = 256;
    public Discoveries { biomes = List.copyOf(biomes); creatures = List.copyOf(creatures); }
    public int size() { return biomes.size() + creatures.size(); }
    public static Discoveries read(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(DATA_KEY);
        if (tag.getInt("Version") != 1) return new Discoveries(List.of(), List.of());
        var registry = player.registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceLocation> biomes = ids(tag.getList("Biomes", Tag.TAG_STRING), registry::containsKey, LIMIT);
        List<ResourceLocation> creatures = ids(tag.getList("Creatures", Tag.TAG_STRING), BuiltInRegistries.ENTITY_TYPE::containsKey, LIMIT - biomes.size());
        return new Discoveries(biomes, creatures);
    }
    private static List<ResourceLocation> ids(ListTag input, Predicate<ResourceLocation> known, int limit) {
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
    private Discoveries added(ResourceLocation id, boolean biome) {
        List<ResourceLocation> selected = biome ? biomes : creatures;
        if (id == null || size() >= LIMIT || selected.contains(id)) return this;
        ArrayList<ResourceLocation> result = new ArrayList<>(selected);
        result.add(id);
        result.sort(ResourceLocation::compareTo);
        return biome ? new Discoveries(result, creatures) : new Discoveries(biomes, result);
    }
    public void save(Player player) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        tag.put("Biomes", strings(biomes));
        tag.put("Creatures", strings(creatures));
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
