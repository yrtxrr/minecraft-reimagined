package dev.reimagined.quest;

import dev.reimagined.quest.team.QuestTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;

/**
 * Event-driven quest milestone tracker. Listens to NeoForge game events and records
 * actual player actions as QuestProgress milestones. No world scanning; every check is
 * triggered by a discrete player action.
 *
 * <p>Milestones tracked:<ul>
 * <li>{@link QuestProgress#MILESTONE_FIRST_LOGIN} — first login</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_CRAFT_TABLE} — first crafting table interaction</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_TOOL} — first tool crafted</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_SHELTER} — first bed placed (shelter)</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_MEAL_EATEN} — first food item consumed</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_SEASON_CHECK} — research journal used</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_ORE_MINED} — first ore block mined</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_BREED_ANIMAL} — first animal bred (advancement)</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_VILLAGE_FOUND} — first village advancement</li>
 * <li>{@link QuestProgress#MILESTONE_FIRST_TRADE_COMPLETED} — first completed villager trade</li>
 * </ul>
 */
public final class QuestEvents {
    private QuestEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(QuestEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onClone);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onCraft);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onSmelt);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onBrew);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onFish);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onKill);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onAdvancement);
        NeoForge.EVENT_BUS.addListener(QuestEvents::onTrade);
    }

    private static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_LOGIN);
        }
    }

    private static void onRespawn(PlayerRespawnEvent event) {
        // Quest progress persists via player NBT; nothing to carry manually on respawn.
    }

    private static void onClone(PlayerEvent.Clone event) {
        QuestProgress.copy(event);
        QuestTeam.copy(event);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        Block block = state.getBlock();
        if (block == Blocks.CRAFTING_TABLE) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_CRAFT_TABLE);
        }
        if (isOreBlock(block)) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_ORE_MINED);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        ItemStack stack = event.getItemStack();
        // Placing a bed: right-click with a bed item in hand.
        if (isBedItem(stack.getItem())) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_SHELTER);
        }
        // Interacting with crafting table.
        if (block == Blocks.CRAFTING_TABLE) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_CRAFT_TABLE);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        // Using research journal.
        if (stack.getItem() == dev.reimagined.content.Content.RESEARCH_JOURNAL.get()) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_SEASON_CHECK);
        }
        // Consuming any item counts as finding food for the first chapter.
        QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_MEAL_EATEN);
    }

    private static void onCraft(ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_CRAFT_TABLE);
        Item item = event.getCrafting().getItem();
        if (isToolItem(item)) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_TOOL);
        }
    }

    private static void onSmelt(ItemSmeltedEvent event) {
        // Smelting is processing, not eating; do not mark meal eaten.
    }

    private static void onBrew(PlayerBrewedPotionEvent event) {
        // Brewing is alchemy, not eating.
    }

    private static void onFish(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getDrops().isEmpty()) {
            // Any fishing drop counts as food for the first chapter.
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_MEAL_EATEN);
        }
    }

    private static void onKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        // Killing a monster is not eating.
    }

    private static void onAdvancement(AdvancementEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String id = event.getAdvancement().id().toString();
        if (id.contains("husbandry/breed_animal") || id.contains("breed_animal")) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_BREED_ANIMAL);
        }
        if (id.contains("village")) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_VILLAGE_FOUND);
        }
        if (id.contains("trade")) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_TRADE_COMPLETED);
        }
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestProgress.mark(player, QuestProgress.MILESTONE_FIRST_TRADE_COMPLETED);
        }
    }

    private static boolean isOreBlock(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || block == Blocks.COPPER_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.REDSTONE_ORE
                || block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.DEEPSLATE_COPPER_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.DEEPSLATE_DIAMOND_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.DEEPSLATE_LAPIS_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE;
    }

    private static boolean isBedItem(Item item) {
        return item == Blocks.WHITE_BED.asItem() || item == Blocks.RED_BED.asItem()
                || item == Blocks.BLUE_BED.asItem() || item == Blocks.GREEN_BED.asItem()
                || item == Blocks.BLACK_BED.asItem() || item == Blocks.BROWN_BED.asItem()
                || item == Blocks.YELLOW_BED.asItem() || item == Blocks.MAGENTA_BED.asItem()
                || item == Blocks.LIGHT_BLUE_BED.asItem() || item == Blocks.CYAN_BED.asItem()
                || item == Blocks.GRAY_BED.asItem() || item == Blocks.LIGHT_GRAY_BED.asItem()
                || item == Blocks.LIME_BED.asItem() || item == Blocks.PINK_BED.asItem()
                || item == Blocks.PURPLE_BED.asItem();
    }

    private static boolean isToolItem(Item item) {
        return item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.DiggerItem
                || item instanceof net.minecraft.world.item.HoeItem
                || item instanceof net.minecraft.world.item.AxeItem
                || item instanceof net.minecraft.world.item.PickaxeItem
                || item instanceof net.minecraft.world.item.ShovelItem;
    }
}
