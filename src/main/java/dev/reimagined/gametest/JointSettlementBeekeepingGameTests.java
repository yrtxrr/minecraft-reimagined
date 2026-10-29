package dev.reimagined.gametest;

import dev.reimagined.beekeeping.*;
import dev.reimagined.settlements.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class JointSettlementBeekeepingGameTests {
    @GameTest(template="empty")
    public static void npcInteractionConsumesSmokerClickAndTradeKeepsHiveStateIndependent(GameTestHelper h) {
        var npc=h.spawn(SettlementContent.SETTLER.get(),new BlockPos(4,2,4));npc.setNoAi(true);
        var player=new SettlementTradingGameTests.Customer(h);
        var pos=new BlockPos(6,2,6);h.setBlock(pos,Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL,5));
        var hive=(BeehiveBlockEntity)h.getBlockEntity(pos);
        for(int n=0;n<3;n++)h.assertTrue(BeeFrames.install(hive),"Each frame installs in the same level as the NPC");
        var smoker=new ItemStack(BeekeepingContent.BEE_SMOKER.get());player.setItemInHand(InteractionHand.MAIN_HAND,smoker);
        var event=new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,npc);NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction() && player.containerMenu instanceof TradingMenu,
            "NPC interaction is consumed rather than falling through to held item use");
        h.assertTrue(smoker.getDamageValue()==0 && BeeFrames.get(hive)==3 && h.getBlockState(pos).getValue(BeehiveBlock.HONEY_LEVEL)==5,
            "Opening trade must not smoke or harvest the adjacent hive");
        var menu=(TradingMenu)player.containerMenu;
        player.getInventory().setItem(1,new ItemStack(Items.STICK,64));
        player.connection.handleSelectTrade(new net.minecraft.network.protocol.game.ServerboundSelectTradePacket(0));
        menu.clicked(2,0,ClickType.QUICK_MOVE,player);
        h.assertTrue(npc.getOffers().get(0).getUses()==2 && player.getInventory().countItem(Items.EMERALD)==2,
            "Ordinary server selection/shift purchase pays twice next to a hive");
        menu.removed(player);player.containerMenu=player.inventoryMenu;
        smoker.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)),Direction.NORTH,h.absolutePos(pos),false)));
        h.assertTrue(smoker.getDamageValue()==1 && BeeFrames.get(hive)==3 && h.getBlockState(pos).getValue(BeehiveBlock.HONEY_LEVEL)==0
            && npc.getOffers().get(0).getUses()==2,"Explicit hive use spends durability and retains independent NPC stock");
        h.succeed();
    }
}
