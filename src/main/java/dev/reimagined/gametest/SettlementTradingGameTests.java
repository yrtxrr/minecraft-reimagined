package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.settlements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class SettlementTradingGameTests {
    public static final class Customer extends ServerPlayer {
        public final List<Packet<?>> packets=new ArrayList<>();
        public Customer(GameTestHelper h) {
            super(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"TradeTest"),net.minecraft.server.level.ClientInformation.createDefault());
            setPos(h.absolutePos(new BlockPos(3,2,3)).getCenter());
            connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),this,
                CommonListenerCookie.createInitial(getGameProfile(),false)) {
                @Override public void send(Packet<?> p) { packets.add(p); }
                @Override public void send(Packet<?> p,PacketSendListener listener) { packets.add(p); }
            };
        }
    }
    private static Settler npc(GameTestHelper h) {
        var s=SettlementContent.SETTLER.get().create(h.getLevel());
        s.setPos(h.absolutePos(new BlockPos(3,2,4)).getCenter());s.setNoAi(true);
        SettlementData.get(h.getLevel()).registerSettler(s.getUUID(),s.blockPosition(),s.blockPosition(),"farmer");
        h.getLevel().addFreshEntity(s);return s;
    }
    private static TradingMenu menu(Settler s,Customer p) {
        s.setTradingPlayer(p);var m=new TradingMenu(1,p.getInventory(),s);p.containerMenu=m;return m;
    }
    @GameTest(template="empty")
    public static void actualSlotsChargeEveryPurchaseAndRejectEmptyOrInsufficientPayment(GameTestHelper h) {
        var s=npc(h);var p=new Customer(h);var m=menu(s,p);
        m.setSelectionHint(0);
        m.clicked(2,0,ClickType.PICKUP,p);
        h.assertTrue(m.getCarried().isEmpty() && s.getOffers().get(0).getUses()==0,"No input means no output or stock change");
        m.getSlot(0).set(new ItemStack(Items.WHEAT,19));m.slotsChanged(m.getSlot(0).container);
        m.clicked(2,0,ClickType.PICKUP,p);
        h.assertTrue(m.getCarried().isEmpty() && m.getSlot(0).getItem().getCount()==19,"Insufficient input cannot pay partially");
        m.getSlot(0).set(new ItemStack(Items.WHEAT,40));m.slotsChanged(m.getSlot(0).container);
        m.clicked(2,0,ClickType.PICKUP,p);m.clicked(2,0,ClickType.PICKUP,p);m.clicked(2,0,ClickType.PICKUP,p);
        h.assertTrue(m.getCarried().is(Items.EMERALD) && m.getCarried().getCount()==2 && m.getSlot(0).getItem().isEmpty()
            && s.getOffers().get(0).getUses()==2,"Repeated requests charge20each and cannot reuse consumed payment");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void shiftClickIsAtomicForFullAndPartiallyFullInventory(GameTestHelper h) {
        var s=npc(h);var p=new Customer(h);var m=menu(s,p);m.setSelectionHint(4);
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        p.getInventory().setItem(0,new ItemStack(Items.BREAD,61));
        m.getSlot(0).set(new ItemStack(Items.EMERALD,2));m.slotsChanged(m.getSlot(0).container);
        m.clicked(2,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getInventory().getItem(0).getCount()==61 && m.getSlot(0).getItem().getCount()==2
            && s.getOffers().get(4).getUses()==0,"Room for only3of6bread must cause no partial insertion, payment or stock mutation");
        p.getInventory().setItem(0,ItemStack.EMPTY);m.clicked(2,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getInventory().getItem(0).is(Items.BREAD) && p.getInventory().getItem(0).getCount()==12
            && m.getSlot(0).getItem().isEmpty() && s.getOffers().get(4).getUses()==2,"Shift-click buys multiple fully paid outputs");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void twoCustomersCannotSpendOneNpcStockConcurrentlyAndOffersAreSent(GameTestHelper h) {
        var s=npc(h);var a=new Customer(h);var b=new Customer(h);
        s.openTradingScreen(a,s.getDisplayName(),1);s.openTradingScreen(b,s.getDisplayName(),1);
        h.assertTrue(a.containerMenu instanceof TradingMenu && !(b.containerMenu instanceof TradingMenu),"Only one active customer per NPC");
        h.assertTrue(a.packets.stream().anyMatch(p->p instanceof ClientboundMerchantOffersPacket),"Real vanilla offer synchronization packet is sent");
        var m=(TradingMenu)a.containerMenu;
        a.getInventory().setItem(0,new ItemStack(Items.WHEAT,20));
        a.connection.handleSelectTrade(new ServerboundSelectTradePacket(0));
        h.assertTrue(m.getSlot(0).getItem().getCount()==20,"Real selection packet loads required payment from server inventory");
        m.clicked(2,0,ClickType.PICKUP,a);
        h.assertTrue(m.getCarried().is(Items.EMERALD),"Synchronized selection buys a paid result");
        m.removed(a);s.openTradingScreen(b,s.getDisplayName(),1);
        h.assertTrue(b.containerMenu instanceof TradingMenu && s.getOffers().get(0).getUses()==1,"Next customer sees shared stock of this NPC");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void stockIsPerUuidPersistsAndFrozenTimeDoesNotRestock(GameTestHelper h) {
        var level=h.getLevel();long old=level.getDayTime();
        try {
            level.setDayTime(8000);var data=TradeData.get(level);var a=UUID.randomUUID();var b=UUID.randomUUID();
            var one=data.offersFor(level,a,"farmer");var two=data.offersFor(level,b,"farmer");
            one.get(0).increaseUses();data.setDirty();
            for(int n=0;n<100;n++)h.assertTrue(data.offersFor(level,a,"farmer").get(0).getUses()==1,"Frozen8000cannot restock used offer");
            h.assertTrue(two.get(0).getUses()==0,"Same profession has independent NPC stock");
            var restored=TradeData.load(data.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());
            h.assertTrue(restored.offersFor(level,a,"farmer").get(0).getUses()==1,"Uses survive real save/load and reentry");
            level.setDayTime(32000);h.assertTrue(restored.offersFor(level,a,"farmer").get(0).getUses()==0,"Next work day restocks once");
            restored.offersFor(level,a,"farmer").get(0).increaseUses();
            h.assertTrue(restored.offersFor(level,a,"farmer").get(0).getUses()==1,"Repeated same-day access cannot restock again");
        } finally {level.setDayTime(old);}
        h.succeed();
    }
}
