package dev.reimagined.creature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

public final class Otter extends Animal {
    public Otter(EntityType<? extends Animal> type, Level level) { super(type, level); setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0); }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this) { @Override public boolean canUse() { return isInLava(); } });
        goalSelector.addGoal(2, new PanicGoal(this, 1.8));
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Wolf.class, 10, 1.4, 1.8));
        goalSelector.addGoal(5, new BreedGoal(this, 1));
        goalSelector.addGoal(6, new TemptGoal(this, 1.2, Ingredient.of(Items.COD, Items.SALMON), false));
        goalSelector.addGoal(7, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(8, new FaunaGoals.SurfaceSwim(this));
        goalSelector.addGoal(9, new FaunaGoals.ShoreRest(this));
        goalSelector.addGoal(10, new FaunaGoals.SeekWater(this));
        goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(13, new RandomLookAroundGoal(this));
    }

    boolean needsRest;
    int swimTicks;
    long nextWaterSearch;
    boolean resting;
    public boolean isResting() { return resting; }
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation(this, level);
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.COD) || stack.is(Items.SALMON); }

    @Override public Otter getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return CreatureContent.OTTER.get().create(level);
    }

}
