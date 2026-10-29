package dev.reimagined.beekeeping;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Portable bee smoker. On a full hive or nest it harvests one honey bottle and one honeycomb per
 * installed frame without releasing the colony; anywhere else it calms nearby angry bees. The
 * entity query is bounded and only runs on an explicit player interaction.
 */
public final class BeeSmokerItem extends Item {
    private static final int HIVE_CALM_RADIUS = 8;
    private static final int AIR_CALM_RADIUS = 6;
    private static final int MAX_CALMED_BEES = 64;

    public BeeSmokerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.BEEHIVES) || !(state.getBlock() instanceof BeehiveBlock hiveBlock)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player != null && player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean harvested = state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS;
        int frames = level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive ? BeeFrames.get(hive) : 0;
        if (harvested) {
            if (player != null) give(player, new ItemStack(Items.HONEY_BOTTLE));
            if (frames > 0) Block.popResource(level, pos, new ItemStack(Items.HONEYCOMB, frames));
            hiveBlock.resetHoneyLevel(level, state, pos);
        }
        int calmed = calm(level, pos, HIVE_CALM_RADIUS);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.3F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.01);
        }
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this, 20);
            if (!harvested && frames == 0 && calmed == 0) {
                player.displayClientMessage(Component.translatable("message.reimagined.bee_smoker_idle"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            int calmed = calm(level, player.blockPosition(), AIR_CALM_RADIUS);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.3, 0.3, 0.01);
            }
            if (calmed > 0) {
                player.displayClientMessage(Component.translatable("message.reimagined.bee_smoker_calm", calmed), true);
            }
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reimagined.bee_smoker"));
    }

    /** Clears anger and targets for bees in a small local box; returns how many were affected. */
    private static int calm(Level level, BlockPos center, int radius) {
        List<Bee> bees = new java.util.ArrayList<>(MAX_CALMED_BEES);
        level.getEntities(net.minecraft.world.entity.EntityType.BEE, new AABB(center).inflate(radius, 6, radius),
                bee -> bee.isAngry() || bee.getTarget() != null, bees, MAX_CALMED_BEES);
        for (Bee bee : bees) {
            bee.stopBeingAngry();
            bee.setTarget(null);
        }
        return bees.size();
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
