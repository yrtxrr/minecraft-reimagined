package dev.reimagined.creature;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Local, bounded searches; optional idle goals always yield to danger, food and breeding. */
final class FaunaGoals {
    private FaunaGoals() {}

    static AvoidEntityGoal<Player> avoidPlayer(Animal animal, float radius, double walk, double sprint) {
        return new AvoidEntityGoal<>(animal, Player.class,
                e -> e instanceof Player p && !animal.isFood(p.getMainHandItem()) && !animal.isFood(p.getOffhandItem()),
                radius, walk, sprint, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    static BlockState state(Animal mob, BlockPos pos) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    // Vanilla path finding can inspect up to FOLLOW_RANGE + 8; fail closed at unloaded borders.
    static boolean ready(Animal mob) {
        var level = (ServerLevel) mob.level();
        var p = mob.blockPosition();
        for (int x = (p.getX()-24)>>4; x <= (p.getX()+24)>>4; x++)
            for (int z = (p.getZ()-24)>>4; z <= (p.getZ()+24)>>4; z++)
                if (level.getChunkSource().getChunkNow(x,z) == null) return false;
        return true;
    }

    static final class SurfaceSwim extends Goal {
        private final Otter mob;
        private int nextDirection;
        private Vec3 target;
        SurfaceSwim(Otter mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return mob.isInWater(); }
        @Override public boolean canContinueToUse() { return mob.isInWater(); }
        @Override public void start() { nextDirection=0; mob.getNavigation().stop(); }
        @Override public void stop() { if(mob.swimTicks>=40) mob.needsRest=true; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            mob.swimTicks++;
            if (--nextDirection <= 0) {
                nextDirection=40;
                target=null;
                BlockPos p=mob.blockPosition();
                // Only 81 loaded positions per 40 ticks, after a full swim interval.
                if (mob.swimTicks >= 160) {
                    double best=Double.MAX_VALUE;
                    for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
                        BlockPos bank=p.offset(x,1,z);
                        var floor=state(mob,bank.below()); var air=state(mob,bank);
                        var above=state(mob,bank.above());
                        if(floor!=null && air!=null && above!=null && floor.blocksMotion() && air.isAir() && above.isAir()) {
                            double d=bank.distToCenterSqr(mob.position());
                            if(d<best) { best=d; target=Vec3.atBottomCenterOf(bank); }
                        }
                    }
                    if(target!=null) mob.needsRest=true;
                }
                if(target==null) {
                    double angle=mob.getRandom().nextDouble()*Math.PI*2;
                    target=mob.position().add(Math.cos(angle)*3,0,Math.sin(angle)*3);
                }
            }
            Vec3 direction=target.subtract(mob.position());
            Vec3 horizontal=new Vec3(direction.x,0,direction.z).normalize().scale(0.025);
            var above=state(mob,mob.blockPosition().above());
            double up=above!=null && above.getFluidState().is(FluidTags.WATER) ? 0.05 : 0.015;
            if(mob.needsRest && direction.horizontalDistanceSqr()<2.5) up=0.10;
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.85,0.8,0.85).add(horizontal.x,up,horizontal.z));
            mob.getLookControl().setLookAt(target.x,mob.getEyeY(),target.z);
        }
    }

    static final class ShoreRest extends Goal {
        private final Otter mob; private int remaining;
        ShoreRest(Otter mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() { return mob.needsRest && !mob.isInWater() && mob.onGround(); }
        @Override public boolean canContinueToUse() { return remaining>0 && !mob.isInWater() && mob.onGround(); }
        @Override public void start() { remaining=100; mob.swimTicks=0; mob.resting=true; mob.getNavigation().stop(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() { remaining--; }
        @Override public void stop() { mob.resting=false; mob.needsRest=false; mob.nextWaterSearch=mob.level().getGameTime()+100; }
    }

    static final class SeekWater extends Goal {
        private final Otter mob; private Path path; private int remaining;
        SeekWater(Otter mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            long now=mob.level().getGameTime();
            if(mob.isInWater() || !mob.onGround() || mob.needsRest || now<mob.nextWaterSearch) return false;
            mob.nextWaterSearch=now+100+mob.getId()%20;
            if(!ready(mob)) return false;
            BlockPos origin=mob.blockPosition(),best=null;double distance=Double.MAX_VALUE;
            // 363 loaded-only cells, one path attempt per five seconds, never a world scan.
            for(int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) for(int y=-2;y<=0;y++) {
                BlockPos p=origin.offset(x,y,z);var water=state(mob,p);var above=state(mob,p.above());
                if(water!=null && above!=null && water.getFluidState().is(FluidTags.WATER) && above.isAir()) {
                    double d=p.distSqr(origin);if(d<distance) {distance=d;best=p;}
                }
            }
            path=best==null?null:mob.getNavigation().createPath(best,0);
            return path!=null && path.canReach();
        }
        @Override public void start() { remaining=160;mob.getNavigation().moveTo(path,1); }
        @Override public boolean canContinueToUse() { return --remaining>0 && !mob.isInWater() && !mob.getNavigation().isDone(); }
        @Override public void stop() { mob.getNavigation().stop();path=null; }
    }

    static final class RainShelter extends Goal {
        private final Badger mob; private long nextSearch; private Path path; private int remaining;
        RainShelter(Badger mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            long now=mob.level().getGameTime();
            if(now<nextSearch || !mob.level().isRaining() || !mob.onGround()) return false;
            nextSearch=now+60+mob.getId()%20;
            if(!ready(mob)) return false;
            if(!mob.level().canSeeSky(mob.blockPosition())) { path=null; return true; }
            BlockPos origin=mob.blockPosition(), best=null; double distance=Double.MAX_VALUE;
            // At most 243 candidates, one path request, once per 60+ staggered ticks.
            for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) for(int y=-1;y<=1;y++) {
                BlockPos p=origin.offset(x,y,z);
                var floor=state(mob,p.below());var air=state(mob,p);var head=state(mob,p.above());
                if(floor!=null && air!=null && head!=null && floor.blocksMotion() && air.isAir() && head.isAir()
                        && !mob.level().canSeeSky(p)) {
                    double d=p.distSqr(origin);
                    if(d<distance) {distance=d;best=p;}
                }
            }
            if(best==null) return false;
            path=mob.getNavigation().createPath(best,0);
            return path!=null && path.canReach();
        }
        @Override public void start() { remaining=400; mob.sheltering=false; if(path!=null) mob.getNavigation().moveTo(path,1.1); }
        @Override public boolean canContinueToUse() {
            return remaining>0 && mob.level().isRaining() && state(mob,mob.blockPosition())!=null
                    && (!mob.getNavigation().isDone() || !mob.level().canSeeSky(mob.blockPosition()));
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() { remaining--; mob.sheltering=!mob.level().canSeeSky(mob.blockPosition()); }
        @Override public void stop() { mob.sheltering=false; mob.getNavigation().stop(); path=null; }
    }

    static final class NightRest extends Goal {
        private final Marmot mob;
        NightRest(Marmot mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return !mob.level().isDay() && mob.onGround() && !mob.isInWater(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { mob.getNavigation().stop(); }
    }

    static final class LookAround extends Goal {
        private final Deer mob; private long nextLook; private int remaining; private double angle;
        LookAround(Deer mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() {
            if(nextLook==0) nextLook=mob.level().getGameTime()+40+mob.getId()%40;
            return mob.level().getGameTime()>=nextLook && mob.onGround() && !mob.isInWater() && mob.getNavigation().isDone();
        }
        @Override public boolean canContinueToUse() { return remaining>0 && !mob.isInWater(); }
        @Override public void start() { remaining=40; angle=Math.toRadians(mob.getYRot());mob.lookingAround=true;mob.getNavigation().stop(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            remaining--;
            double a=angle+(remaining>20 ? -0.8 : 0.8);
            mob.getLookControl().setLookAt(mob.getX()-Math.sin(a)*3,mob.getEyeY(),mob.getZ()+Math.cos(a)*3);
        }
        @Override public void stop() { mob.lookingAround=false; nextLook=mob.level().getGameTime()+160+mob.getRandom().nextInt(200); }
    }
}
