package dev.reimagined.gametest;

import dev.reimagined.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CoreProgressGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("test",path); }
    @GameTest(template="empty")
    public static void twoPlayersHaveIndependentResearchAndNonlinearEras(GameTestHelper h) {
        var a=new SettlementTradingGameTests.Customer(h); var b=new SettlementTradingGameTests.Customer(h);
        h.assertTrue(CoreApi.research(a,id("ruins")) && !CoreApi.research(a,id("ruins")),"Research transition is idempotent");
        h.assertTrue(CoreApi.unlock(a,CoreProgress.Era.COMPUTING),"Computing need not force every preceding quest");
        h.assertTrue(CoreApi.progress(b).knowledge().isEmpty() && CoreApi.progress(b).eras().isEmpty(),"No static cross-player progress");
        h.assertTrue(CoreApi.progress(a).eras().size()==1,"No hard linear ladder"); h.succeed();
    }
    @GameTest(template="empty")
    public static void progressSurvivesCloneAndNbtRoundTripWithoutTouchingLegacy(GameTestHelper h) {
        var original=new SettlementTradingGameTests.Customer(h);var replacement=new SettlementTradingGameTests.Customer(h);
        original.getPersistentData().putString("legacy_sentinel","untouched"); CoreApi.research(original,id("trade"));
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement,original,true));
        h.assertTrue(CoreApi.progress(replacement).knowledge().contains(id("trade")),"Research survives death clone");
        var disk=CoreProgress.load(CoreApi.progress(replacement).save());
        h.assertTrue(disk.knowledge().contains(id("trade")) && original.getPersistentData().getString("legacy_sentinel").equals("untouched"),"NBT retains ID and unrelated saves");
        CoreApi.research(replacement,id("magic"));
        h.assertTrue(!CoreApi.progress(original).knowledge().contains(id("magic")),"Clone tag is deeply copied");h.succeed();
    }
    @GameTest(template="empty")
    public static void boundedCorruptAndFutureDataAreHandledExplicitly(GameTestHelper h) {
        var state=new CoreProgress(); for(int n=0;n<CoreProgress.LIMIT+100;n++)state.learn(id("knowledge_"+n));
        h.assertTrue(state.knowledge().size()==CoreProgress.LIMIT,"Finite save budget");
        var tag=new CompoundTag(); tag.putInt("Version",1);var list=new ListTag();list.add(StringTag.valueOf("INVALID ID"));list.add(StringTag.valueOf("test:valid"));tag.put("Knowledge",list);
        h.assertTrue(CoreProgress.load(tag).knowledge().size()==1,"Malformed IDs rejected");
        tag.putInt("Version",2);boolean refused=false;try {CoreProgress.load(tag);}catch(IllegalStateException expected){refused=true;}
        h.assertTrue(refused,"Future schema is not silently overwritten");h.succeed();
    }
    @GameTest(template="empty")
    public static void questSignalIsOnlyEmittedOnActualResearchTransition(GameTestHelper h) {
        var player=new SettlementTradingGameTests.Customer(h);int[] calls={0};
        java.util.function.Consumer<CoreSignal> listener=e -> {if(player.getUUID().equals(e.actor()) && e.subject().equals(id("experiment")))calls[0]++;};
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            CoreApi.research(player,id("experiment"));CoreApi.research(player,id("experiment"));
            h.assertTrue(calls[0]==1,"One reward event, no repeated/no-op quest completion");
        }finally {NeoForge.EVENT_BUS.unregister(listener);}
        h.succeed();
    }
}
