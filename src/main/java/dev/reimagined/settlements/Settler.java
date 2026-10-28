package dev.reimagined.settlements;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * A settlement citizen NPC. Settlers are not vanilla villagers; they follow a
 * bounded daily schedule, return to their workplace, take short breaks, and
 * remain inside their home settlement bounds.
 */
public final class Settler extends Mob {
    private static final int WORK_RADIUS = 48;
    private static final int BREAK_CHANCE = 2000;
    private static final int WALK_CHANCE = 4000;
    private static final int WALK_DURATION_TICKS = 400;

    private BlockPos homePos = BlockPos.ZERO;
    private BlockPos workPos = BlockPos.ZERO;
    private int scheduleTick;
    private Schedule.State scheduleState = Schedule.State.IDLE;
    private int breakTimer;
    private int walkTimer;
    private boolean atWorkplace;

    public Settler(EntityType<? extends Mob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    public BlockPos homePos() { return homePos; }
    public void setHome(BlockPos pos) { this.homePos = pos; }
    public BlockPos workPos() { return workPos; }
    public void setWork(BlockPos pos) { this.workPos = pos; }
    public Schedule.State scheduleState() { return scheduleState; }
    public boolean atWorkplace() { return atWorkplace; }
    public void setAtWorkplace(boolean value) { this.atWorkplace = value; }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ReturnToWorkplaceGoal(this));
        goalSelector.addGoal(2, new ScheduleWalkGoal(this));
        goalSelector.addGoal(3, new BreakGoal(this));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        scheduleTick++;
        if (scheduleTick % 20 == 0) {
            Schedule.State next = Schedule.nextState(this, scheduleState, scheduleTick);
            if (next != scheduleState) {
                scheduleState = next;
                atWorkplace = false;
            }
        }
        if (breakTimer > 0) breakTimer--;
        if (walkTimer > 0) walkTimer--;

        // Keep movement bounded: pull the settler back toward home if it strays too far.
        if (homePos != null && homePos.distSqr(blockPosition()) > WORK_RADIUS * WORK_RADIUS
                && !getNavigation().isInProgress()) {
            getNavigation().moveTo(homePos.getX(), homePos.getY(), homePos.getZ(), 1.0);
        }
    }

    /** Returns true if the block at pos is walkable ground inside settlement bounds. */
    public boolean isInSettlementBounds(BlockPos pos) {
        return homePos.distSqr(pos) <= WORK_RADIUS * WORK_RADIUS;
    }

    /** Returns the nearest safe ground position within the settlement, for returning home after chunk reload. */
    public BlockPos nearestSafePosition(BlockPos center) {
        BlockPos best = center;
        int bestScore = Integer.MAX_VALUE;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                BlockPos candidate = center.offset(dx, 0, dz);
                if (!isInSettlementBounds(candidate)) continue;
                if (!level().getBlockState(candidate.below()).isSolid()) continue;
                int score = Math.abs(dx) + Math.abs(dz);
                if (score < bestScore) { best = candidate; bestScore = score; }
            }
        }
        return best;
    }

    private BlockPos randomWalkTarget() {
        int attempts = 16;
        for (int i = 0; i < attempts; i++) {
            BlockPos candidate = homePos.offset(
                    level().random.nextInt(25) - 12,
                    0,
                    level().random.nextInt(25) - 12);
            if (!isInSettlementBounds(candidate)) continue;
            if (!level().getBlockState(candidate.below()).isSolid()) continue;
            return candidate;
        }
        return null;
    }

    /* ---------- AI goals ---------- */

    private static final class ReturnToWorkplaceGoal extends Goal {
        private final Settler settler;
        ReturnToWorkplaceGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return settler.scheduleState == Schedule.State.WORK && settler.workPos != null
                    && settler.blockPosition().distSqr(settler.workPos) > 4;
        }
        @Override public boolean canContinueToUse() { return canUse() && settler.atWorkplace; }
        @Override public void start() {
            settler.getNavigation().moveTo(settler.workPos.getX(), settler.workPos.getY(), settler.workPos.getZ(), 1.0);
        }
        @Override public void tick() {
            if (settler.blockPosition().distSqr(settler.workPos) <= 4) settler.setAtWorkplace(true);
        }
    }

    private static final class ScheduleWalkGoal extends Goal {
        private final Settler settler;
        ScheduleWalkGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (settler.scheduleState != Schedule.State.IDLE && settler.scheduleState != Schedule.State.BREAK) return false;
            if (settler.level().random.nextInt(WALK_CHANCE) != 0) return false;
            return settler.walkTimer <= 0;
        }
        @Override public boolean canContinueToUse() { return settler.walkTimer > 0; }
        @Override public void start() {
            BlockPos target = settler.randomWalkTarget();
            if (target != null) {
                settler.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
                settler.walkTimer = WALK_DURATION_TICKS;
            }
        }
    }

    private static final class BreakGoal extends Goal {
        private final Settler settler;
        BreakGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return settler.scheduleState == Schedule.State.WORK && settler.breakTimer <= 0
                    && settler.level().random.nextInt(BREAK_CHANCE) == 0;
        }
        @Override public void start() {
            settler.scheduleState = Schedule.State.BREAK;
            settler.breakTimer = 200;
        }
    }

    @Override
    public boolean canFreeze() { return false; }

    @Override
    public void setCustomName(Component name) { super.setCustomName(name); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HomeX", homePos.getX());
        tag.putInt("HomeY", homePos.getY());
        tag.putInt("HomeZ", homePos.getZ());
        tag.putInt("WorkX", workPos.getX());
        tag.putInt("WorkY", workPos.getY());
        tag.putInt("WorkZ", workPos.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.homePos = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        this.workPos = new BlockPos(tag.getInt("WorkX"), tag.getInt("WorkY"), tag.getInt("WorkZ"));
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
}
