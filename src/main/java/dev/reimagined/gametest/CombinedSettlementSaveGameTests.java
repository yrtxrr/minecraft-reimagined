package dev.reimagined.gametest;

import dev.reimagined.civilization.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CombinedSettlementSaveGameTests {
    @GameTest(template="empty")
    public static void oneOwnerPreservesNpcAndCivilizationDataAndDirtyPropagation(GameTestHelper h) {
        var level=h.getLevel();var owner=dev.reimagined.settlements.SettlementData.get(level);
        h.assertTrue(SettlementData.get(level.getServer())==owner.civilizations(),"Civilization facade must use the existing world owner");
        var fixture=new dev.reimagined.settlements.SettlementData();var npc=java.util.UUID.randomUUID();
        fixture.registerSettler(npc,new BlockPos(1,2,3),new BlockPos(4,5,6),"farmer");
        var settlement=fixture.civilizations().found(level,SettlementType.AGRICULTURAL,"SharedSave",new BlockPos(25000,80,-25000)).settlement();
        var saved=fixture.save(new CompoundTag(),level.registryAccess());
        var loaded=dev.reimagined.settlements.SettlementData.load(saved,level.registryAccess());
        h.assertTrue(loaded.findSettler(npc)!=null && loaded.civilizations().byId(settlement.id()).isPresent(),"One roundtrip retains both schemas");
        loaded.setDirty(false);loaded.civilizations().remove(settlement.id());
        h.assertTrue(loaded.isDirty(),"Civilization changes dirty the single SavedData owner");
        var oldNpc=saved.copy();oldNpc.remove("Aggregates");oldNpc.putString("Version","1");
        h.assertTrue(dev.reimagined.settlements.SettlementData.load(oldNpc,level.registryAccess()).findSettler(npc)!=null,"Existing NPC-only world remains readable");
        var oldWorld=fixture.civilizations().save(new CompoundTag(),level.registryAccess());
        h.assertTrue(dev.reimagined.settlements.SettlementData.load(oldWorld,level.registryAccess()).civilizations().size()==1,"Freebuff-only top-level saves migrate without a second registry");
        h.succeed();
    }
}
