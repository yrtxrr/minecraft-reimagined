package dev.reimagined.gametest;

import dev.reimagined.quest.QuestIntegration;
import dev.reimagined.core.*;
import dev.reimagined.civilization.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class QuestProgressGameTests {
    private static boolean completed(net.minecraft.server.level.ServerPlayer p,String key) {
        var a=p.server.getAdvancements().get(QuestIntegration.advancement(key));
        return a!=null && p.getAdvancements().getOrStartProgress(a).isDone();
    }
    @GameTest(template="empty")
    public static void actualCraftAndFoodEventsCompleteOnlyTheRightCriteria(GameTestHelper h) {
        var p=new SettlementTradingGameTests.Customer(h);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemSmeltedEvent(p,new ItemStack(Items.IRON_INGOT)));
        h.assertTrue(!completed(p,"first_meal"),"Smelting metal cannot claim food progress");
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(p,new ItemStack(Items.WOODEN_PICKAXE),new SimpleContainer(9)));
        h.assertTrue(completed(p,"first_craft")&&completed(p,"first_tool"),"Real craft event awards actual server advancement criteria");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(p,new ItemStack(Items.BREAD),32,ItemStack.EMPTY));
        h.assertTrue(completed(p,"first_meal"),"Actual food consumption event completes eating criterion");
        h.assertTrue(!QuestIntegration.mark(p,"unbounded_unknown_key"),"Unknown milestone cannot grow NBT without limit");h.succeed();
    }
    @GameTest(template="empty")
    public static void paidTradeCompletesTaskButOpeningAndInsufficientPaymentDoNot(GameTestHelper h) {
        var npc=h.spawn(dev.reimagined.settlements.SettlementContent.SETTLER.get(),new BlockPos(4,2,4));npc.setNoAi(true);
        var player=new SettlementTradingGameTests.Customer(h);npc.openTradingScreen(player,npc.getDisplayName(),1);
        h.assertTrue(!completed(player,"first_trade"),"Menu opening is not a trade");
        var menu=(dev.reimagined.settlements.TradingMenu)player.containerMenu;menu.setSelectionHint(0);
        var cost=npc.getOffers().getFirst().getItemCostA();
        menu.getSlot(0).set(cost.itemStack().copyWithCount(cost.count()-1));menu.slotsChanged(menu.getSlot(0).container);
        menu.clicked(2,0,ClickType.PICKUP,player);
        h.assertTrue(!completed(player,"first_trade")&&menu.getCarried().isEmpty(),"Failed payment grants neither item nor quest");
        menu.getSlot(0).set(cost.itemStack().copy());menu.slotsChanged(menu.getSlot(0).container);menu.clicked(2,0,ClickType.PICKUP,player);
        h.assertTrue(completed(player,"first_trade")&&!menu.getCarried().isEmpty(),"Paid result transaction completes the task");
        menu.removed(player);h.succeed();
    }
    @GameTest(template="empty")
    public static void personalCoreResearchAndCloneDoNotMergePlayers(GameTestHelper h) {
        var a=new SettlementTradingGameTests.Customer(h);var b=new SettlementTradingGameTests.Customer(h);
        var signal=new CoreSignal(CoreSignal.Kind.RESEARCH,ResourceLocation.parse("test:experiment"),a.getUUID());
        QuestIntegration.acceptCore(a,signal);QuestIntegration.acceptCore(b,signal);
        h.assertTrue(completed(a,"first_research")&&!completed(b,"first_research"),"Research stays with its real actor");
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(b,a,true));
        h.assertTrue(QuestIntegration.hasMilestone(b,"first_research"),"Milestone NBT survives the actual clone event");
        QuestIntegration.mark(b,"first_tool");h.assertTrue(!QuestIntegration.hasMilestone(a,"first_tool"),"Clone tag is deeply copied");h.succeed();
    }
    @GameTest(template="empty")
    public static void sharedSettlementSignalsPersistAndReplayWithoutAnotherRegistry(GameTestHelper h) {
        var owner=dev.reimagined.settlements.SettlementData.get(h.getLevel());int before=owner.civilizations().size();
        NeoForge.EVENT_BUS.post(new SettlementQuestEvent(QuestSignalType.SETTLEMENT_GREW,java.util.UUID.randomUUID(),"EventOnly",BlockPos.ZERO,1,25,TechEra.FRONTIER));
        h.assertTrue(owner.questSignals().contains("settlement_grew")&&owner.civilizations().size()==before,"Stable string event records a fact, not another population registry");
        var disk=dev.reimagined.settlements.SettlementData.load(owner.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(disk.questSignals().contains("settlement_grew"),"Shared fact survives v2 save/reload");
        var a=new SettlementTradingGameTests.Customer(h);var b=new SettlementTradingGameTests.Customer(h);
        QuestIntegration.restoreWorldProgress(a);QuestIntegration.restoreWorldProgress(b);
        h.assertTrue(completed(a,"settlement_grew")&&completed(b,"settlement_grew"),"Both late participants receive the shared-world criterion");
        h.assertTrue(!completed(a,"first_craft")&&!completed(b,"first_craft"),"Shared events never synthesize personal crafts");h.succeed();
    }
    @GameTest(template="empty")
    public static void journalUseAndVanillaSleepAwardActualCriteria(GameTestHelper h) {
        var player=new SettlementTradingGameTests.Customer(h);
        var book=new ItemStack(dev.reimagined.content.Content.RESEARCH_JOURNAL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,book);book.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(completed(player,"journal_read"),"Actual journal use awards reading, not possession");
        h.assertTrue(!completed(player,"first_shelter"),"Reading cannot claim shelter");
        var sleep=player.server.getAdvancements().get(ResourceLocation.parse("minecraft:adventure/sleep_in_bed"));
        h.assertTrue(sleep!=null,"Native sleep criterion exists");
        for(String criterion:sleep.value().criteria().keySet())player.getAdvancements().award(sleep,criterion);
        h.assertTrue(completed(player,"first_shelter"),"Completed native sleep advancement drives shelter event");h.succeed();
    }
    @GameTest(template="empty")
    public static void oreCriterionRejectsCanceledBreaksAndMissingHarvestTool(GameTestHelper h) {
        var player=new SettlementTradingGameTests.Customer(h);var pos=h.absolutePos(new BlockPos(5,2,5));
        var state=net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        var canceled=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,state,player);canceled.setCanceled(true);NeoForge.EVENT_BUS.post(canceled);
        h.assertTrue(!completed(player,"first_ore"),"Canceled break cannot complete mining");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,state,player));
        h.assertTrue(!completed(player,"first_ore"),"Bare-handed iron ore is not a harvested resource");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),pos,state,player));
        h.assertTrue(completed(player,"first_ore"),"Approved break with correct tool completes ore criterion");h.succeed();
    }
    @GameTest(template="empty")
    public static void installedChapterUsesNativeTasksWithResolvableAdvancements(GameTestHelper h) throws Exception {
        try(var in=QuestProgressGameTests.class.getResourceAsStream("/data/reimagined/quest_book/chapters/reimagined_intro.snbt")) {
            h.assertTrue(in!=null,"Native config chapter is generated from the one editable source");
            var chapter=TagParser.parseTag(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            var quests=chapter.getList("quests",10);h.assertTrue(quests.size()==8,"All8Kilo chapter intentions retained");
            var ids=new java.util.HashSet<String>();ids.add(chapter.getString("id"));
            for(int n=0;n<quests.size();n++) {
                var q=quests.getCompound(n);h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(q.getCompound("icon").getString("id"))),"Quest icon resolves in real registry");h.assertTrue(q.getString("id").matches("[0-9A-F]{16}")&&ids.add(q.getString("id")),"Stable unique native hex quest IDs");
                var task=q.getList("tasks",10).getCompound(0);h.assertTrue(ids.add(task.getString("id")),"Unique task ID");
                if(task.getString("type").equals("advancement"))h.assertTrue(h.getLevel().getServer().getAdvancements().get(ResourceLocation.parse(task.getString("advancement")))!=null,"Task references a real loaded advancement");
                else h.assertTrue(n==7 && task.getString("type").equals("checkmark"),"Only deliberate path confirmation is manual");
            }
        }h.succeed();
    }
}
