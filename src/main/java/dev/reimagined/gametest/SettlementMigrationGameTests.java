package dev.reimagined.gametest;

import dev.reimagined.settlements.SettlementData;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class SettlementMigrationGameTests {
    private static CompoundTag a() {
        return parse("""
          {Version:"1", NextSettlerId:3, Settlers:[
           {Uuid:[I;11259375,2002870789,-1430532899,11259375],HomeX:120,HomeY:64,HomeZ:-40,WorkX:128,WorkY:64,WorkZ:-40,Profession:"farmer",TradeTick:1400,Stock:6},
           {Uuid:[I;843991,-2002870789,1430532899,-843991],HomeX:118,HomeY:64,HomeZ:-36,WorkX:130,WorkY:64,WorkZ:-44,Profession:"librarian",TradeTick:0,Stock:0}]}
          """);
    }
    private static CompoundTag b() {
        // Handoff unsigned UUID words 2596069104/2271560481 have these identical signed int bits.
        return parse("""
          {Version:1,Settlements:[{Version:1,Id:[I;305419896,-1698898192,-19088744,-2023406815],Name:"Millford",Type:"agricultural",Era:"SETTLED",X:112,Y:63,Z:-48,Population:34,Housing:40,Food:512,Materials:208,Knowledge:240,Treasury:96,FoundedDay:12L,LastGameTime:240000L,FoundedAnnounced:1b,FirstTradeAnnounced:1b,EraAnnouncedMask:3,GrowthMilestone:4,HousingMilestone:1}]}
          """);
    }
    private static CompoundTag parse(String snbt) { try{return TagParser.parseTag(snbt);}catch(Exception e){throw new IllegalStateException(e);} }
    private static SettlementData load(GameTestHelper h,CompoundTag tag){return SettlementData.load(tag,h.getLevel().registryAccess());}
    private static CompoundTag save(GameTestHelper h,SettlementData data){return data.save(new CompoundTag(),h.getLevel().registryAccess());}
    @GameTest(template="empty")
    public static void codexV1SettlersSurviveMigration(GameTestHelper h) {
        var source=a();var state=load(h,source);var output=save(h,state);
        h.assertTrue(output.contains("Version",3)&&output.getInt("Version")==2,"Canonical version is integer2");
        h.assertTrue(source.getList("Settlers",10).equals(output.getList("Settlers",10)) && output.getInt("NextSettlerId")==3,"Exact NPC fields and sequence preserved");
        h.assertTrue(output.getList("Aggregates",10).isEmpty()&&!state.migrationWarning().isEmpty(),"Missing aggregate tier reported, never invented");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void freebuffV1AggregatesSurviveMigration(GameTestHelper h) {
        var source=b();var state=load(h,source);var output=save(h,state);
        h.assertTrue(source.getList("Settlements",10).equals(output.getList("Aggregates",10)),"All economy, era, clock and announcement fields survive exactly");
        h.assertTrue(output.getList("Settlers",10).isEmpty()&&!state.migrationWarning().isEmpty(),"Missing NPC tier reported, no synthetic NPCs/trades");h.succeed();
    }
    @GameTest(template="empty")
    public static void mergedTagKeepsBothTiersAndReloadIsIdempotent(GameTestHelper h) {
        var combined=a();combined.put("Settlements",b().getList("Settlements",10));
        var first=load(h,combined);h.assertTrue(!first.isDirty(),"Loader does not force save");
        var canonical=save(h,first);var second=load(h,canonical);var third=load(h,save(h,second));
        h.assertTrue(canonical.equals(save(h,third))&&!second.isDirty()&&!third.isDirty(),"Repeated reload creates no entries, clock changes or dirty writes");
        h.assertTrue(third.settlers().size()==2&&third.civilizations().size()==1,"Both tiers remain independent without duplication");
        h.assertTrue(!canonical.contains("Settlements")&&!canonical.contains("Civilizations"),"Legacy aggregate key removed from v2");h.succeed();
    }
    @GameTest(template="empty")
    public static void duplicateUuidsKeepFirstWithinEachTier(GameTestHelper h) {
        var source=a();var duplicate=source.getList("Settlers",10).getCompound(0).copy();duplicate.putString("Profession","overwritten");source.getList("Settlers",10).add(duplicate);
        var aggregates=b().getList("Settlements",10);var dupe=aggregates.getCompound(0).copy();dupe.putString("Name","overwritten");aggregates.add(dupe);source.put("Settlements",aggregates);
        var loaded=load(h,source);
        h.assertTrue(loaded.settlers().size()==2&&loaded.settlers().getFirst().profession.equals("farmer"),"First NPC record wins");
        h.assertTrue(loaded.civilizations().size()==1&&loaded.civilizations().all().iterator().next().name().equals("Millford"),"First aggregate wins without population doubling");
        h.assertTrue(loaded.migrationWarning().contains("duplicate"),"Duplicate discard is reported");h.succeed();
    }
    @GameTest(template="empty")
    public static void unknownProfessionAndEraAndMissingVersionAreSafe(GameTestHelper h) {
        var source=a();source.remove("Version");source.getList("Settlers",10).getCompound(0).putString("Profession","future_profession");
        var aggregates=b().getList("Settlements",10);aggregates.getCompound(0).putString("Era","future_era");source.put("Settlements",aggregates);
        var loaded=load(h,source);
        h.assertTrue(loaded.settlers().getFirst().profession.equals("future_profession"),"Unknown profession is never erased");
        var catalogue=new dev.reimagined.settlements.TradeData();
        catalogue.addOffer("commoner",new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK,32),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD),16);
        h.assertTrue(!catalogue.offersFor(h.getLevel(),loaded.settlers().getFirst().uuid,"future_profession").isEmpty(),"Unrecognized stored profession gets safe commoner offers without erasing its ID");
        h.assertTrue(loaded.civilizations().all().iterator().next().era()==dev.reimagined.civilization.TechEra.FRONTIER,"Unknown era follows existing safe fallback");
        var future=save(h,loaded);future.putInt("Version",3);boolean refused=false;try{load(h,future);}catch(IllegalStateException expected){refused=true;}
        h.assertTrue(refused,"Future save must not be overwritten by older codec");h.succeed();
    }
}
