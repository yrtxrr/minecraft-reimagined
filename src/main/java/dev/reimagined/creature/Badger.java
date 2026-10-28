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

public final class Badger extends Animal {
    private int shelterTimer;

    public Badger(EntityType<? extends Animal> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BadgerRainShelterGoal(this));
        goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 12, 1.25, 1.6));
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 10, 1.2, 1.5));
        goalSelector.addGoal(5, new BreedGoal(this, 1));
        goalSelector.addGoal(6, new TemptGoal(this, 1.0, Ingredient.of(Items.SWEET_BERRIES, Items.APPLE), false));
        goalSelector.addGoal(7, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override public void tick() {
        super.tick();
        if (shelterTimer > 0) shelterTimer--;
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.SWEET_BERRIES) || stack.is(Items.APPLE); }

    @Override public Badger getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return CreatureContent.BADGER.get().create(level);
    }

    static final class BadgerRainShelterGoal extends Goal {
        private final Badger badger;
        BadgerRainShelterGoal(Badger badger) { this.badger = badger; }
        @Override public boolean canUse() {
            return badger.level().isRaining() && badger.shelterTimer == 0 && badger.getRandom().nextInt(200) == 0;
        }
        @Override public boolean canContinueToUse() { return badger.shelterTimer > 0 && badger.level().isRaining(); }
        @Override public void start() { badger.shelterTimer = badger.getRandom().nextInt(100, 200); badger.getNavigation().stop(); }
        @Override public void stop() { badger.shelterTimer = 0; }
        @Override public void tick() {
            if (badger.shelterTimer > 0) badger.getLookControl().setLookAt(badger.getX(), badger.getY() + 1, badger.getZ());
        }
    }
}
