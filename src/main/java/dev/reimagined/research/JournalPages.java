package dev.reimagined.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

/** Translatable components stay translatable over the wire, so each reader uses their own language. */
public final class JournalPages {
    private JournalPages() {}
    public static WrittenBookContent build(ServerPlayer reader) {
        Discoveries discoveries = Discoveries.read(reader);
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page.introduction", discoveries.biomes().size(), discoveries.creatures().size())));
        var current = reader.serverLevel().getBiome(reader.blockPosition());
        ResourceLocation currentId = current.unwrapKey().orElseThrow().location();
        pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page.current_biome",
                Component.translatableWithFallback("biome." + currentId.getNamespace() + "." + currentId.getPath().replace('/', '.'), currentId.getPath().replace('_', ' ')),
                String.format(java.util.Locale.ROOT, "%.2f", current.value().getBaseTemperature()))));
        for (String chapter : List.of("seasons", "skills", "recipes", "engineering", "magic", "combat", "wildlife", "cooking"))
            pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page." + chapter)));
        catalogue(pages, discoveries.biomes(), "biomes", "biome");
        catalogue(pages, discoveries.creatures(), "creatures", "entity");
        // 10 guide pages + at most 87 catalogue pages for the combined discovery cap of 256.
        return new WrittenBookContent(Filterable.passThrough("Minecraft: Reimagined"), "Reimagined", 0, List.copyOf(pages), true);
    }
    private static void catalogue(List<Filterable<Component>> pages, List<ResourceLocation> entries, String chapter, String prefix) {
        if (entries.isEmpty()) {
            pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page." + chapter)
                    .append("\n\n").append(Component.translatable("journal.reimagined.page.undiscovered"))));
            return;
        }
        for (int start = 0; start < entries.size(); start += 3) {
            MutableComponent page = Component.translatable("journal.reimagined.page." + chapter).append("\n");
            for (ResourceLocation id : entries.subList(start, Math.min(entries.size(), start + 3))) {
                String fallback = id.getPath().replace('_', ' ');
                if (fallback.length() > 36) fallback = fallback.substring(0, 33) + "...";
                page.append("\n• ").append(Component.translatableWithFallback(
                        prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'), fallback));
            }
            pages.add(Filterable.passThrough(page));
        }
    }
}
