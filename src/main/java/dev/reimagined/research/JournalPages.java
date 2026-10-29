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
        pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page.introduction", discoveries.biomes().size(), discoveries.creatures().size(), discoveries.blocks().size())));
        var current = reader.serverLevel().getBiome(reader.blockPosition());
        ResourceLocation currentId = current.unwrapKey().orElseThrow().location();
        pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page.current_biome",
                Component.translatableWithFallback("biome." + currentId.getNamespace() + "." + currentId.getPath().replace('/', '.'), currentId.getPath().replace('_', ' ')),
                String.format(java.util.Locale.ROOT, "%.2f", current.value().getBaseTemperature()))));
        for (String chapter : List.of("seasons", "skills", "recipes", "engineering", "magic", "combat", "wildlife", "cooking", "orchard", "beekeeping"))
            pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page." + chapter)));
        for(int part=0;part<2;part++) {
            var first=dev.reimagined.magic.Spell.values()[4+part*2];
            var second=dev.reimagined.magic.Spell.values()[5+part*2];
            pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.spell_progress",
                Integer.bitCount(dev.reimagined.magic.SpellKnowledge.mask(reader)),
                Component.translatable(first.translationKey()),spellStatus(reader,first),
                Component.translatable(second.translationKey()),spellStatus(reader,second))));
        }
        int pageSize=JournalPageBudget.entriesPerPage(discoveries.biomes().size(),discoveries.creatures().size(),discoveries.blocks().size());
        catalogue(pages, discoveries.biomes(), "biomes", "biome",pageSize);
        catalogue(pages, discoveries.creatures(), "creatures", "entity",pageSize);
        catalogue(pages, discoveries.blocks(), "blocks", "block",pageSize);
        // Keep normal three-entry pages; pack four only when the three catalogues would reach100pages.
        return new WrittenBookContent(Filterable.passThrough("Minecraft: Reimagined"), "Reimagined", 0, List.copyOf(pages), true);
    }
    private static Component spellStatus(ServerPlayer player,dev.reimagined.magic.Spell spell) {
        return Component.translatable(dev.reimagined.magic.SpellKnowledge.knows(player,spell)?"reimagined.magic.learned":"reimagined.magic.locked");
    }
    private static void catalogue(List<Filterable<Component>> pages, List<ResourceLocation> entries, String chapter, String prefix,int pageSize) {
        if (entries.isEmpty()) {
            pages.add(Filterable.passThrough(Component.translatable("journal.reimagined.page." + chapter)
                    .append("\n\n").append(Component.translatable("journal.reimagined.page.undiscovered"))));
            return;
        }
        for (int start = 0; start < entries.size(); start += pageSize) {
            MutableComponent page = Component.translatable("journal.reimagined.page." + chapter).append("\n");
            for (ResourceLocation id : entries.subList(start, Math.min(entries.size(), start + pageSize))) {
                String fallback = id.getPath().replace('_', ' ');
                if (fallback.length() > 36) fallback = fallback.substring(0, 33) + "...";
                page.append("\n• ").append(Component.translatableWithFallback(
                        prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'), fallback));
            }
            pages.add(Filterable.passThrough(page));
        }
    }
}
