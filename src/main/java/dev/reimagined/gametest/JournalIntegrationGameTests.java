package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.research.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class JournalIntegrationGameTests {
    private static FakePlayer reader(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"JournalIntegration"));
        p.setPos(h.absolutePos(new BlockPos(5,2,8)).getCenter().add(0,-.5,0));p.setYRot(180);p.setXRot(0);return p;
    }
    @GameTest(template="empty")
    public static void lookedAtRareBlockIsDiscoveredButNotThroughWalls(GameTestHelper h) {
        var p=reader(h);h.setBlock(new BlockPos(5,3,4),Blocks.CALCITE);h.setBlock(new BlockPos(5,3,6),Blocks.STONE);
        ResearchEvents.observe(p);h.assertTrue(Discoveries.read(p).blocks().isEmpty(),"Hidden ore/block must not be discovered through a wall");
        h.setBlock(new BlockPos(5,3,6),Blocks.AIR);ResearchEvents.observe(p);
        h.assertTrue(Discoveries.read(p).blocks().equals(List.of(ResourceLocation.parse("minecraft:calcite"))),"Loaded-only sight ray records the actual rare block");
        var first=Discoveries.read(p);ResearchEvents.observe(p);h.assertTrue(Discoveries.read(p).equals(first),"Repeated observation is idempotent");h.succeed();
    }
    @GameTest(template="empty")
    public static void oldSavesAndAllThreeCataloguesSurviveDeathCloneAndReentry(GameTestHelper h) {
        var p=reader(h);new Discoveries(List.of(ResourceLocation.parse("minecraft:plains")),List.of(ResourceLocation.parse("minecraft:cow"))).save(p);
        p.getPersistentData().getCompound(Discoveries.DATA_KEY).remove("Blocks");
        h.assertTrue(Discoveries.read(p).blocks().isEmpty() && Discoveries.read(p).size()==2,"Old v1saves without Blocks retain both existing catalogues");
        Discoveries.read(p).withBlock(ResourceLocation.parse("minecraft:calcite")).save(p);var expected=Discoveries.read(p);
        var reload=reader(h);reload.load(p.saveWithoutId(new CompoundTag()));
        h.assertTrue(Discoveries.read(reload).equals(expected),"Actual player serialization/reentry retains all three catalogues");
        var deadReplacement=reader(h);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(deadReplacement,reload,true));
        h.assertTrue(Discoveries.read(deadReplacement).equals(expected),"Registered death clone handler preserves rare blocks too");
        deadReplacement.getPersistentData().getCompound(Discoveries.DATA_KEY).getList("Blocks",Tag.TAG_STRING).clear();
        h.assertTrue(Discoveries.read(reload).equals(expected),"Death clone owns a deep copy of block NBT");
        var endReplacement=reader(h);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(endReplacement,reload,false));
        h.assertTrue(Discoveries.read(endReplacement).equals(expected),"Nondeath clone also retains discoveries");
        var book=JournalPages.build(endReplacement);
        h.assertTrue(book.pages().stream().anyMatch(page->net.minecraft.network.chat.Component.Serializer.toJson(page.raw(),h.getLevel().registryAccess()).contains("block.minecraft.calcite")),"Rare block appears in the actual journal, not just hidden NBT");h.succeed();
    }
    @GameTest(template="empty")
    public static void fullBlockCatalogueFitsVanillaBookAndFiltersUnknownIds(GameTestHelper h) {
        var p=reader(h);var ids=BuiltInRegistries.BLOCK.keySet().stream().sorted().limit(256).toList();
        new Discoveries(List.of(),List.of(),ids).save(p);
        var content=JournalPages.build(p);
        h.assertTrue(Discoveries.read(p).size()==256 && content.pages().size()<100,"All256known block discoveries fit with guide and empty biome/creature headings");
        net.minecraft.world.item.component.WrittenBookContent.CODEC.encodeStart(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),content).getOrThrow();
        var changed=new CompoundTag();changed.putInt("Version",1);var blocks=new ListTag();blocks.add(StringTag.valueOf("missing:unknown_block"));blocks.add(StringTag.valueOf("minecraft:calcite"));blocks.add(StringTag.valueOf("minecraft:calcite"));changed.put("Blocks",blocks);p.getPersistentData().put(Discoveries.DATA_KEY,changed);
        h.assertTrue(Discoveries.read(p).blocks().equals(List.of(ResourceLocation.parse("minecraft:calcite"))),"Malformed/duplicate/unknown block IDs cannot pollute discoveries");h.succeed();
    }
}
