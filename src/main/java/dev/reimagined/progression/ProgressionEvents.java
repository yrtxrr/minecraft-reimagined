package dev.reimagined.progression;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Common-side only: safe to load on a dedicated server. */
public final class ProgressionEvents {
    private ProgressionEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onClone);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onCraft);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onSmelt);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onBrew);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onTrade);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onFish);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onKill);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onCommands);
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(PlayerSkills.DATA_KEY)) {
            event.getEntity().getPersistentData().put(PlayerSkills.DATA_KEY,
                    event.getOriginal().getPersistentData().getCompound(PlayerSkills.DATA_KEY).copy());
        }
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        var state = event.getState();
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            PlayerSkills.award(player, Skill.FARMER, 8);
        } else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            PlayerSkills.award(player, Skill.MINER, 2);
        }
    }

    private static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var item = event.getCrafting().getItem();
        // Each event is a completed craft, not a crafting-grid preview.
        if (item instanceof ArmorItem || item instanceof SwordItem || item instanceof DiggerItem) {
            PlayerSkills.award(player, Skill.SMITH, 12);
        } else {
            // General crafting trains engineering; the boost is finite and does not duplicate items.
            PlayerSkills.award(player, Skill.ENGINEER, Math.min(16, event.getCrafting().getCount()));
        }
    }

    private static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerSkills.award(player, Skill.SMITH,
                Math.min(64, event.getSmelting().getCount()) * 2L);
    }

    private static void onBrew(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerSkills.award(player, Skill.ALCHEMIST, 15);
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerSkills.award(player, Skill.MERCHANT, 12);
    }

    private static void onFish(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getDrops().isEmpty()) {
            PlayerSkills.award(player, Skill.SAILOR, 10);
        }
    }

    private static void onKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Enemy) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        Skill skill = attackSkill(event.getSource());
        if (skill != null) PlayerSkills.award(player, skill, 20);
    }

    private static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getEntity() instanceof net.minecraft.world.entity.player.Player) return;
        Skill skill = attackSkill(event.getSource());
        if (skill == null) return;
        event.setNewDamage((float) (event.getNewDamage() * PlayerSkills.bonusMultiplier(player, skill)));
    }

    private static Skill attackSkill(DamageSource source) {
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return Skill.MAGE;
        if (source.getDirectEntity() instanceof Projectile) return Skill.ARCHER;
        if (source.is(DamageTypes.PLAYER_ATTACK)) return Skill.WARRIOR;
        return null;
    }

    private static void onCommands(RegisterCommandsEvent event) {
        var unlock = Commands.literal("unlock").then(Commands.argument("tree", StringArgumentType.word())
                .suggests((context, builder) -> {
                    Arrays.stream(Skill.values()).map(Skill::id)
                            .filter(id -> id.startsWith(builder.getRemainingLowerCase())).forEach(builder::suggest);
                    return builder.buildFuture();
                }).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    Skill skill;
                    try {
                        skill = Skill.valueOf(StringArgumentType.getString(context, "tree").toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        throw new SimpleCommandExceptionType(Component.translatable("message.reimagined.unknown_skill")).create();
                    }
                    SkillProgress before = PlayerSkills.get(player, skill);
                    if (before.rank() == SkillProgress.MAX_RANK) {
                        throw new SimpleCommandExceptionType(Component.translatable("message.reimagined.skill_mastered")).create();
                    }
                    if (before.availablePoints().signum() <= 0) {
                        throw new SimpleCommandExceptionType(Component.translatable("message.reimagined.skill_no_points")).create();
                    }
                    SkillProgress after = before.unlock();
                    PlayerSkills.set(player, skill, after);
                    context.getSource().sendSuccess(() -> Component.translatable("message.reimagined.skill_unlocked",
                            Component.translatable(skill.translationKey()), after.rank(), SkillProgress.MAX_RANK), false);
                    return 1;
                }));
        event.getDispatcher().register(Commands.literal("reimagined").then(Commands.literal("skills")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    for (Skill skill : Skill.values()) {
                        SkillProgress state = PlayerSkills.get(player, skill);
                        context.getSource().sendSuccess(() -> Component.translatable("message.reimagined.skill_summary",
                                Component.translatable(skill.translationKey()), state.level().toString(), state.rank(),
                                SkillProgress.MAX_RANK, state.availablePoints().toString(), state.toNextLevel().toString()), false);
                    }
                    return 1;
                }).then(unlock)));
    }
}
