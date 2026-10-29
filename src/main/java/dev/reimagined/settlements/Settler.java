package dev.reimagined.settlements;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * A settlement citizen NPC. Settlers are not vanilla villagers; they follow a
 * bounded daily schedule, return to their workplace, take short breaks, and
 * remain inside their home settlement bounds.
 */
public final class Settler extends Mob implements MenuProvider, net.minecraft.world.item.trading.Merchant {
    private static final int WORK_RADIUS = 48;
    private static final int BREAK_CHANCE = 2000;
    private static final int WALK_CHANCE = 4000;
    private static final int WALK_DURATION_TICKS = 400;
    private static final int RAIN_ENTER_TICKS = 200;
    private static final int RAIN_EXIT_TICKS = 400;

    private BlockPos homePos = BlockPos.ZERO;
    private BlockPos workPos = BlockPos.ZERO;
    private int scheduleTick;
    private Schedule.State scheduleState = Schedule.State.IDLE;
    private int breakTimer;
    private int walkTimer;
    private boolean atWorkplace;
    private int rainTimer;
    private int nextShelterSearch;

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
    public int rainTimer() { return rainTimer; }
    public void setRainTimer(int value) { this.rainTimer = value; }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Goal() {
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse() { return tradingPlayer!=null; }
            @Override public boolean canContinueToUse() { return tradingPlayer!=null; }
            @Override public void start() { getNavigation().stop(); }
        });
        goalSelector.addGoal(2, new ReturnToWorkplaceGoal(this));
        goalSelector.addGoal(3, new ReturnHomeGoal(this));
        goalSelector.addGoal(4, new ShelterGoal(this));
        goalSelector.addGoal(5, new NightRestGoal(this));
        goalSelector.addGoal(6, new ScheduleWalkGoal(this));
        goalSelector.addGoal(7, new BreakGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        scheduleTick++;
        
        // Track rain duration for shelter hysteresis
        boolean raining = level().isRaining();
        if (raining) {
            rainTimer = Math.min(rainTimer + 1, RAIN_ENTER_TICKS);
        } else {
            rainTimer = Math.max(rainTimer - 1, -RAIN_EXIT_TICKS);
        }
        
        if ((scheduleTick + getId()) % 20 == 0) {
            Schedule.State next = Schedule.nextState(this, scheduleState, scheduleTick);
            if (next != scheduleState) {
                scheduleState = next;
                atWorkplace = false;
            }
        }
        if (breakTimer > 0) breakTimer--;
        if (walkTimer > 0) walkTimer--;

        // Keep movement bounded: pull the settler back toward home if it strays too far.
        if (tradingPlayer == null && pathReady() && homePos != null && homePos.distSqr(blockPosition()) > WORK_RADIUS * WORK_RADIUS
                && !getNavigation().isInProgress()) {
            getNavigation().moveTo(homePos.getX(), homePos.getY(), homePos.getZ(), 1.0);
        }
    }

    /** Returns true if the block at pos is walkable ground inside settlement bounds. */
    public boolean isInSettlementBounds(BlockPos pos) {
        return homePos.distSqr(pos) <= WORK_RADIUS * WORK_RADIUS;
    }

    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new SettlerNavigation(this,level);
    }
    private boolean pathReady() { return ((SettlerNavigation)getNavigation()).ready(); }
    private boolean isChunkLoaded(BlockPos pos) {
        return level() instanceof ServerLevel server && server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private BlockState getBlockStateSafe(BlockPos pos) {
        if (!(level() instanceof ServerLevel server)) return null;
        var chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk==null?null:chunk.getBlockState(pos);
    }

    /** Checks if the position has solid ground below it. */
    private boolean hasSolidGround(BlockPos pos) {
        BlockState state = getBlockStateSafe(pos.below());
        var feet=getBlockStateSafe(pos);var head=getBlockStateSafe(pos.above());
        return state != null && state.isSolid() && feet!=null && feet.isAir() && head!=null && head.isAir();
    }

    /** Returns the nearest safe ground position within the settlement, for returning home after chunk reload. */
    public BlockPos nearestSafePosition(BlockPos center) {
        BlockPos best = center;
        int bestScore = Integer.MAX_VALUE;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                BlockPos candidate = center.offset(dx, 0, dz);
                if (!isInSettlementBounds(candidate)) continue;
                if (!hasSolidGround(candidate)) continue;
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
            if (!hasSolidGround(candidate)) continue;
            return candidate;
        }
        return null;
    }

    private boolean isCovered(BlockPos pos) {
        if (!(level() instanceof ServerLevel server)) return false;
        var chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null && chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
            pos.getX()&15,pos.getZ()&15)>pos.getY()+1;
    }
    private BlockPos findShelterPosition() {
        if (hasSolidGround(homePos) && isCovered(homePos)) return homePos;
        for(int n=0;n<16;n++) {
            var p=homePos.offset(level().random.nextInt(17)-8,0,level().random.nextInt(17)-8);
            if(isInSettlementBounds(p) && hasSolidGround(p) && isCovered(p))return p;
        }
        return null;
    }

    /* ---------- AI goals ---------- */

    private static final class ReturnToWorkplaceGoal extends Goal {
        private final Settler settler;
        ReturnToWorkplaceGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return settler.pathReady() && settler.tradingPlayer == null && settler.scheduleState == Schedule.State.WORK && settler.workPos != null
                    && settler.blockPosition().distSqr(settler.workPos) > 4;
        }
        @Override public boolean canContinueToUse() {
            return settler.tradingPlayer == null && settler.scheduleState == Schedule.State.WORK && settler.workPos != null
                    && settler.blockPosition().distSqr(settler.workPos) > 4
                    && settler.getNavigation().isInProgress();
        }
        @Override public void start() {
            settler.getNavigation().moveTo(settler.workPos.getX(), settler.workPos.getY(), settler.workPos.getZ(), 1.0);
        }
        @Override public void tick() {
            if (settler.blockPosition().distSqr(settler.workPos) <= 4) settler.setAtWorkplace(true);
            if (settler.pathReady() && !settler.getNavigation().isInProgress() && settler.blockPosition().distSqr(settler.workPos) > 4) {
                settler.setAtWorkplace(false);
            }
        }
    }

    private static final class ScheduleWalkGoal extends Goal {
        private final Settler settler;
        ScheduleWalkGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (!settler.pathReady() || settler.tradingPlayer != null) return false;
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
            return settler.pathReady() && settler.tradingPlayer == null && settler.scheduleState == Schedule.State.WORK && settler.breakTimer <= 0
                    && settler.level().random.nextInt(BREAK_CHANCE) == 0;
        }
        @Override public void start() {
            settler.scheduleState = Schedule.State.BREAK;
            settler.breakTimer = 200;
        }
    }

    private static final class ReturnHomeGoal extends Goal {
        private final Settler settler;
        ReturnHomeGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return settler.pathReady() && settler.tradingPlayer == null && settler.scheduleState == Schedule.State.EVENING_RETURN && settler.homePos != null
                    && settler.blockPosition().distSqr(settler.homePos) > 4;
        }
        @Override public boolean canContinueToUse() {
            return settler.tradingPlayer == null && settler.scheduleState == Schedule.State.EVENING_RETURN && settler.homePos != null
                    && settler.blockPosition().distSqr(settler.homePos) > 4
                    && settler.getNavigation().isInProgress();
        }
        @Override public void start() {
            settler.getNavigation().moveTo(settler.homePos.getX(), settler.homePos.getY(), settler.homePos.getZ(), 1.0);
        }
        @Override public void tick() {
            if (settler.pathReady() && !settler.getNavigation().isInProgress() && settler.blockPosition().distSqr(settler.homePos) > 4) {
                BlockPos safePos = settler.nearestSafePosition(settler.homePos);
                if (safePos != null && safePos.distSqr(settler.blockPosition()) > 1) {
                    settler.getNavigation().moveTo(safePos.getX(), safePos.getY(), safePos.getZ(), 1.0);
                }
            }
        }
    }

    private static final class ShelterGoal extends Goal {
        private final Settler settler;
        ShelterGoal(Settler s) { settler=s;setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return settler.tradingPlayer==null && settler.pathReady() && settler.tickCount>=settler.nextShelterSearch
                && settler.scheduleState==Schedule.State.SHELTER && !settler.isCovered(settler.blockPosition());
        }
        @Override public boolean canContinueToUse() {
            return settler.tradingPlayer==null && settler.scheduleState==Schedule.State.SHELTER
                && settler.getNavigation().isInProgress() && !settler.isCovered(settler.blockPosition());
        }
        @Override public void start() {
            var target=settler.findShelterPosition();settler.nextShelterSearch=settler.tickCount+40;
            if(target!=null)settler.getNavigation().moveTo(target.getX(),target.getY(),target.getZ(),1);
        }
    }

    private static final class NightRestGoal extends Goal {
        private final Settler settler;
        NightRestGoal(Settler settler) { this.settler = settler; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return settler.pathReady() && settler.tradingPlayer == null && settler.scheduleState == Schedule.State.NIGHT_REST && settler.homePos != null
                    && settler.blockPosition().distSqr(settler.homePos) > 2;
        }
        @Override public boolean canContinueToUse() {
            return settler.tradingPlayer == null && settler.scheduleState == Schedule.State.NIGHT_REST && settler.homePos != null
                    && settler.blockPosition().distSqr(settler.homePos) > 2
                    && settler.getNavigation().isInProgress();
        }
        @Override public void start() {
            BlockPos safePos = settler.nearestSafePosition(settler.homePos);
            settler.getNavigation().moveTo(safePos.getX(), safePos.getY(), safePos.getZ(), 0.8);
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.reimagined.settler");
    }

    private Player tradingPlayer;
    private net.minecraft.world.item.trading.MerchantOffers activeOffers;

    @Override public void setTradingPlayer(Player player) {
        tradingPlayer = player;
        if (player == null) activeOffers = null;
    }
    @Override public Player getTradingPlayer() { return tradingPlayer; }
    @Override public net.minecraft.world.item.trading.MerchantOffers getOffers() {
        if (!(level() instanceof ServerLevel server)) return new net.minecraft.world.item.trading.MerchantOffers();
        if (activeOffers == null) {
            var record = SettlementData.get(server).findSettler(getUUID());
            activeOffers = TradeData.get(server).offersFor(server,getUUID(),record == null ? "commoner" : record.profession);
        }
        return activeOffers;
    }
    @Override public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers) {}
    @Override public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
        if (level() instanceof ServerLevel server && getOffers().contains(offer)) {
            offer.increaseUses(); TradeData.get(server).setDirty();
        }
    }
    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int value) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return true; }
    @Override public boolean isClientSide() { return level().isClientSide; }
    @Override public net.minecraft.sounds.SoundEvent getNotifyTradeSound() { return net.minecraft.sounds.SoundEvents.VILLAGER_YES; }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TradingMenu(id, inventory, this);
    }

    @Override public void openTradingScreen(Player player, Component title, int merchantLevel) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer) || !isAlive()
                || player.isSpectator() || player.distanceToSqr(this)>64 || tradingPlayer != null) return;
        setTradingPlayer(player);
        var menuId = player.openMenu(this);
        if (menuId.isPresent()) player.sendMerchantOffers(menuId.getAsInt(),getOffers(),1,0,false,true);
        else setTradingPlayer(null);
    }
}
