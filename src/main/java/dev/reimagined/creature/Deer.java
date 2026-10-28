package dev.reimagined.creature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public final class Deer extends Animal {
    private int lookAroundTimer;

    public Deer(EntityType<? extends Animal> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.7));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Wolf.class, 12, 1.3, 1.7));
        goalSelector.addGoal(3, new BreedGoal(this, 1));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, Ingredient.of(Items.SWEET_BERRIES), false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(7, new DeerLookAroundGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override public void tick() {
        super.tick();
        if (lookAroundTimer > 0) lookAroundTimer--;
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.SWEET_BERRIES); }

    @Override public Deer getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return CreatureContent.DEER.get().create(level);
    }

    static final class DeerLookAroundGoal extends Goal {
        private final Deer deer;
        DeerLookAroundGoal(Deer deer) { this.deer = deer; }
        @Override public boolean canUse() {
            return deer.lookAroundTimer == 0 && deer.getRandom().nextInt(800) == 0 && !deer.isInWater();
        }
        @Override public boolean canContinueToUse() { return deer.lookAroundTimer > 0; }
        @Override public void start() { deer.lookAroundTimer = 40; deer.getNavigation().stop(); }
        @Override public void stop() { deer.lookAroundTimer = 0; }
        @Override public void tick() {
            if (deer.lookAroundTimer > 0) {
                deer.getLookControl().setLookAt(deer.getX() + deer.getRandom().nextDouble() * 2 - 1,
                        deer.getY() + 1, deer.getZ() + deer.getRandom().nextDouble() * 2 - 1);
            }
        }
    }
}
