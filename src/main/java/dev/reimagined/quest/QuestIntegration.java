package dev.reimagined.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;

/**
 * Lightweight quest event hooks for FTB Quests / KubeJS integration.
 * Listens to game events and records player progress in persistent data.
 * No world scanning, no per-tick iteration. All checks are event-driven.
 */
public final class QuestIntegration {
    private static final String DATA_KEY = "reimagined_quests_v1";

    private QuestIntegration() {}

    /** Player-triggered milestone: first log-in of the world. */
    public static final String MILESTONE_FIRST_LOGIN = "first_login";
    public static final String MILESTONE_FIRST_CRAFT = "first_craft";
    public static final String MILESTONE_FIRST_TOOL = "first_tool";
    public static final String MILESTONE_FIRST_SHELTER = "first_shelter";
    public static final String MILESTONE_FIRST_MEAL = "first_meal";
    public static final String MILESTONE_FIRST_BREED = "first_breed";
    public static final String MILESTONE_FIRST_ORE = "first_ore";
    public static final String MILESTONE_FIRST_VILLAGE = "first_village";
    public static final String MILESTONE_FIRST_TRADE = "first_trade";

    private static void ensure(ServerPlayer player) {
        var data = player.getPersistentData();
        if (!data.contains(DATA_KEY)) data.put(DATA_KEY, new net.minecraft.nbt.CompoundTag());
    }

    public static void mark(ServerPlayer player, String milestone) {
        ensure(player);
        var tags = player.getPersistentData().getCompound(DATA_KEY);
        tags.putBoolean(milestone, true);
        player.getPersistentData().put(DATA_KEY, tags);
    }

    public static boolean has(ServerPlayer player, String milestone) {
        ensure(player);
        return player.getPersistentData().getCompound(DATA_KEY).getBoolean(milestone);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onLogin);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onRespawn);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onClone);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onBreak);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onInteract);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onCraft);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onSmelt);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onBrew);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onFish);
        NeoForge.EVENT_BUS.addListener(QuestIntegration::onKill);
    }

    private static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) mark(player, MILESTONE_FIRST_LOGIN);
    }

    private static void onRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Quest progress persists via player NBT; nothing to carry manually.
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer player
                && original.getPersistentData().contains(DATA_KEY)) {
            player.getPersistentData().put(DATA_KEY,
                    original.getPersistentData().getCompound(DATA_KEY).copy());
        }
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        var state = event.getState();
        var block = state.getBlock();
        if (block == net.minecraft.world.level.block.Blocks.CRAFTING_TABLE) mark(player, MILESTONE_FIRST_SHELTER);
    }

    private static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (block == net.minecraft.world.level.block.Blocks.CRAFTING_TABLE) mark(player, MILESTONE_FIRST_CRAFT);
    }

    private static void onCraft(ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        mark(player, MILESTONE_FIRST_CRAFT);
        var item = event.getCrafting().getItem();
        if (item instanceof net.minecraft.world.item.TieredItem || item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.DiggerItem || item instanceof net.minecraft.world.item.HoeItem
                || item instanceof net.minecraft.world.item.AxeItem || item instanceof net.minecraft.world.item.PickaxeItem
                || item instanceof net.minecraft.world.item.ShovelItem) {
            mark(player, MILESTONE_FIRST_TOOL);
        }
    }

    private static void onSmelt(ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) mark(player, MILESTONE_FIRST_MEAL);
    }

    private static void onBrew(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) mark(player, MILESTONE_FIRST_MEAL);
    }

    private static void onFish(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getDrops().isEmpty()) mark(player, MILESTONE_FIRST_MEAL);
    }

    private static void onKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        mark(player, MILESTONE_FIRST_MEAL);
    }

    /** Query helper for KubeJS: returns true if the player has reached a milestone. */
    public static boolean hasMilestone(Player player, String milestone) {
        if (!(player instanceof ServerPlayer sp)) return false;
        return has(sp, milestone);
    }
}
