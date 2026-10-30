package dev.reimagined.quest;

import dev.reimagined.core.*;
import dev.reimagined.civilization.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.Set;

/** Server actions -> personal vanilla advancements -> FTB native advancement tasks. No FTB class dependency. */
public final class QuestIntegration {
    public static final String DATA_KEY="reimagined_quests_v1";
    private static final Set<String> PERSONAL=Set.of("first_login","first_craft","first_tool","first_shelter","first_meal",
        "first_breed","first_ore","first_trade","journal_read","first_research","first_era");
    private QuestIntegration() {}
    public static boolean known(String id) {
        if(PERSONAL.contains(id))return true;
        for(var type:QuestSignalType.values())if(type.id.equals(id))return true;
        return false;
    }
    public static ResourceLocation advancement(String id) { return ResourceLocation.fromNamespaceAndPath("reimagined","quests/"+id); }
    public static boolean hasMilestone(Player player,String id) {
        return known(id) && player instanceof ServerPlayer && player.getPersistentData().getCompound(DATA_KEY).getBoolean(id);
    }
    /** Idempotent evidence only. No items/XP/team state are granted by this API. */
    public static boolean mark(ServerPlayer player,String id) {
        CoreApi.requireServerThread(player.serverLevel());
        if(!known(id))return false;
        boolean changed=!hasMilestone(player,id);
        if(changed) {
            var tag=player.getPersistentData().getCompound(DATA_KEY);tag.putBoolean(id,true);player.getPersistentData().put(DATA_KEY,tag);
        }
        var advancement=player.server.getAdvancements().get(advancement(id));
        if(advancement!=null && !player.getAdvancements().getOrStartProgress(advancement).isDone())
            player.getAdvancements().award(advancement,"milestone");
        return changed;
    }
    public static void register() {
        NeoForge.EVENT_BUS.addListener(QuestIntegration::login);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::clonePlayer);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::craft);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::eat);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, QuestIntegration::ore);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::trade);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::earned);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::breed);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::settlement);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::core);
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {mark(player,"first_login");restoreWorldProgress(player);}
    }
    public static void restoreWorldProgress(ServerPlayer player) {
        for(String id:dev.reimagined.settlements.SettlementData.get(player.serverLevel()).questSignals())mark(player,id);
    }
    private static void clonePlayer(PlayerEvent.Clone event) {
        if(event.getEntity() instanceof ServerPlayer && event.getOriginal().getPersistentData().contains(DATA_KEY))
            event.getEntity().getPersistentData().put(DATA_KEY,event.getOriginal().getPersistentData().getCompound(DATA_KEY).copy());
    }
    private static void craft(PlayerEvent.ItemCraftedEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && !event.getCrafting().isEmpty()) {
            mark(player,"first_craft");if(event.getCrafting().getItem() instanceof TieredItem)mark(player,"first_tool");
        }
    }
    private static void eat(LivingEntityUseItemEvent.Finish event) {
        if(event.getEntity() instanceof ServerPlayer player && event.getItem().has(DataComponents.FOOD))mark(player,"first_meal");
    }
    private static void ore(BlockEvent.BreakEvent event) {
        if(!event.isCanceled() && event.getPlayer() instanceof ServerPlayer player && event.getState().is(Tags.Blocks.ORES) && player.hasCorrectToolForDrops(event.getState()))mark(player,"first_ore");
    }
    private static void trade(TradeWithVillagerEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)mark(player,"first_trade");
    }
    private static void earned(AdvancementEvent.AdvancementEarnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && event.getAdvancement().id().toString().equals("minecraft:adventure/sleep_in_bed"))mark(player,"first_shelter");
    }
    private static void breed(BabyEntitySpawnEvent event) {
        if(!event.isCanceled() && event.getCausedByPlayer() instanceof ServerPlayer player)mark(player,"first_breed");
    }
    private static void settlement(SettlementQuestEvent event) {
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null || !server.isSameThread())throw new IllegalStateException("Settlement quest signals must be posted on the current server thread");
        String id=event.kind().id; // Stable string contract, never enum ordinal or settlement name.
        var data=dev.reimagined.settlements.SettlementData.get(server.overworld());data.recordQuestSignal(id);
        for(var player:server.getPlayerList().getPlayers())mark(player,id); // max2 configured players, transition-only.
    }
    private static void core(CoreSignal event) {
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server!=null && event.actor()!=null) {
            var player=server.getPlayerList().getPlayer(event.actor());if(player!=null)acceptCore(player,event);
        }
    }
    public static void acceptCore(ServerPlayer player,CoreSignal event) {
        CoreApi.requireServerThread(player.serverLevel());
        if(!player.getUUID().equals(event.actor()))return;
        if(event.kind()==CoreSignal.Kind.RESEARCH)mark(player,"first_research");
        else if(event.kind()==CoreSignal.Kind.ERA)mark(player,"first_era");
    }
}
